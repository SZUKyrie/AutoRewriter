package org.autorewriter.rewriter.rule.util;

import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.LogicalInSubFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks output column identities (as {@link ColumnRef}) for each RelNode.
 * This is AutoRewriter's equivalent of WeTune's ValuesRegistry — it provides
 * stable column identity that survives structural plan changes.
 *
 * <p>For each RelNode, {@code outputColumnsOf(node)} returns a list of ColumnRefs
 * representing the identity of each output column:
 * <ul>
 *   <li>TableScan: each column → ColumnRef(tableName, colName)</li>
 *   <li>Project: each output → resolved from projection expression + child registry</li>
 *   <li>Filter/InSubFilter: same as child (filtering doesn't change columns)</li>
 *   <li>Join: left columns + right columns concatenated</li>
 * </ul>
 *
 * <p>Results are cached per RelNode instance for efficiency.
 */
public class ColumnRefRegistry {

    private final Map<RelNode, List<ColumnRef>> cache = new HashMap<>();

    /**
     * Get the output column identities for the given node.
     */
    public List<ColumnRef> outputColumnsOf(RelNode node) {
        node = unwrap(node);
        List<ColumnRef> cached = cache.get(node);
        if (cached != null) return cached;

        List<ColumnRef> result = compute(node);
        cache.put(node, result);
        return result;
    }

    /**
     * Resolve a list of ColumnRefs to positional indices in the target node's output.
     * Returns -1 for any ColumnRef that cannot be resolved.
     * This is the equivalent of WeTune's rebindRefs().
     */
    public List<Integer> resolveIndices(List<ColumnRef> refs, RelNode target) {
        List<ColumnRef> targetCols = outputColumnsOf(target);
        List<Integer> indices = new ArrayList<>();
        for (ColumnRef ref : refs) {
            int idx = targetCols.indexOf(ref);
            if (idx < 0) {
                // Fallback: match by column name only (for cases where table name differs)
                idx = findByColumnName(ref, targetCols);
            }
            indices.add(idx);
        }
        return indices;
    }

    /**
     * Resolve a single ColumnRef to a positional index in the target node's output.
     */
    public int resolveIndex(ColumnRef ref, RelNode target) {
        List<ColumnRef> targetCols = outputColumnsOf(target);
        int idx = targetCols.indexOf(ref);
        if (idx < 0) {
            idx = findByColumnName(ref, targetCols);
        }
        return idx;
    }

    private List<ColumnRef> compute(RelNode node) {
        if (node instanceof TableScan) {
            return computeTableScan((TableScan) node);
        }
        if (node instanceof Project) {
            return computeProject((Project) node);
        }
        if (node instanceof LogicalInSubFilter) {
            // InSubFilter output = left child's output (it's a semi-join filter)
            return outputColumnsOf(((LogicalInSubFilter) node).getLeft());
        }
        if (node instanceof Filter) {
            // Filter preserves child's columns
            return outputColumnsOf(((Filter) node).getInput());
        }
        if (node instanceof Join) {
            return computeJoin((Join) node);
        }
        // Fallback: use ColumnRefResolver for unknown node types
        return computeFallback(node);
    }

    private List<ColumnRef> computeTableScan(TableScan scan) {
        List<String> qualifiedName = scan.getTable().getQualifiedName();
        String tableName = qualifiedName.get(qualifiedName.size() - 1);
        List<ColumnRef> result = new ArrayList<>();
        for (RelDataTypeField field : scan.getRowType().getFieldList()) {
            result.add(new ColumnRef(tableName, field.getName()));
        }
        return result;
    }

    private List<ColumnRef> computeProject(Project project) {
        List<ColumnRef> childCols = outputColumnsOf(project.getInput());
        List<ColumnRef> result = new ArrayList<>();
        List<String> fieldNames = project.getRowType().getFieldNames();

        for (int i = 0; i < project.getProjects().size(); i++) {
            RexNode expr = project.getProjects().get(i);
            if (expr instanceof RexInputRef) {
                int idx = ((RexInputRef) expr).getIndex();
                if (idx < childCols.size()) {
                    result.add(childCols.get(idx));
                } else {
                    result.add(new ColumnRef("$expr", fieldNames.get(i)));
                }
            } else {
                // Complex expression — use field name as identity
                result.add(new ColumnRef("$expr", fieldNames.get(i)));
            }
        }
        return result;
    }

    /**
     * Compute join output columns by concatenating left + right.
     *
     * <p>When the right side contains ColumnRefs that already exist on the left
     * (e.g., self-join on the same table), the right-side refs are disambiguated
     * by appending a positional tag to the table name. This mirrors WeTune's
     * approach where Value objects have unique identity — two columns with the
     * same table.column name from different join branches are distinct Values.
     * Without this, {@link #resolveIndex} would incorrectly resolve a reference
     * from an eliminated table to a same-named column in a surviving table.
     */
    private List<ColumnRef> computeJoin(Join join) {
        List<ColumnRef> leftCols = outputColumnsOf(join.getLeft());
        List<ColumnRef> rightCols = outputColumnsOf(join.getRight());
        List<ColumnRef> result = new ArrayList<>(leftCols.size() + rightCols.size());
        result.addAll(leftCols);

        // Disambiguate right-side columns that collide with left-side columns
        java.util.Set<ColumnRef> leftSet = new java.util.HashSet<>(leftCols);
        for (ColumnRef rightCol : rightCols) {
            if (leftSet.contains(rightCol)) {
                // Tag with positional offset to create a unique identity
                result.add(new ColumnRef(
                        rightCol.getTableName() + "$" + result.size(),
                        rightCol.getColumnName()));
            } else {
                result.add(rightCol);
            }
        }
        return result;
    }

    private List<ColumnRef> computeFallback(RelNode node) {
        List<ColumnRef> result = new ArrayList<>();
        for (int i = 0; i < node.getRowType().getFieldCount(); i++) {
            ColumnRef ref = ColumnRefResolver.resolve(i, node);
            result.add(ref);
        }
        return result;
    }

    /**
     * Fallback resolution by column name. Only matches non-expression columns
     * whose table name is NOT disambiguated (no positional tag).
     * Disambiguated refs (from duplicate table instances in joins) must resolve
     * via exact identity match — falling back to name-only would defeat the
     * purpose of disambiguation.
     */
    private static int findByColumnName(ColumnRef ref, List<ColumnRef> cols) {
        // Never fall back to name-only for disambiguated refs
        if (ref.getTableName().contains("$")) return -1;
        for (int i = 0; i < cols.size(); i++) {
            if (cols.get(i).getColumnName().equalsIgnoreCase(ref.getColumnName())
                    && !cols.get(i).getTableName().startsWith("$")
                    && !cols.get(i).getTableName().contains("$")) {
                return i;
            }
        }
        return -1;
    }

    private static RelNode unwrap(RelNode node) {
        while (node instanceof HepRelVertex || node instanceof RelSubset) {
            if (node instanceof HepRelVertex) {
                node = ((HepRelVertex) node).getCurrentRel();
            } else {
                RelNode best = ((RelSubset) node).getBest();
                if (best != null) {
                    node = best;
                } else {
                    // No best yet — use the original (first) RelNode in the set
                    node = ((RelSubset) node).getOriginal();
                    if (node == null) break;
                }
            }
        }
        return node;
    }
}
