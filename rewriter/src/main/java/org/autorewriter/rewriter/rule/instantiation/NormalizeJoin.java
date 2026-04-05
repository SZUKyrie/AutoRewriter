package org.autorewriter.rewriter.rule.instantiation;

import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rel.logical.LogicalJoin;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.autorewriter.rewriter.rule.util.ColumnRefRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Normalizes join trees to left-deep form by rotating right-deep patterns.
 * Aligned with WeTune's {@code NormalizeJoin} which runs after each rule
 * instantiation to ensure consistent join tree shape.
 *
 * <p>The rotation transforms:
 * <pre>
 *   join0(A, join1(B, C))  =>  join1(join0(A, B), C)   [Pattern 1: keys in B]
 *   join0(A, join1(B, C))  =>  join1(join0(A, C), B)   [Pattern 2: keys in C]
 * </pre>
 *
 * <p>This is applied recursively until the entire join tree is left-deep,
 * meaning no join has another join as its right child.
 */
public class NormalizeJoin {

    private final ColumnRefRegistry registry;

    public NormalizeJoin() {
        this.registry = new ColumnRefRegistry();
    }

    /**
     * Normalize the join tree rooted at {@code root} to left-deep form.
     * Processes the entire tree, normalizing join subtrees found anywhere.
     */
    public RelNode normalize(RelNode root) {
        return normalizeTree(unwrap(root));
    }

    private RelNode normalizeTree(RelNode node) {
        node = unwrap(node);

        // Recursively normalize children first (bottom-up)
        List<RelNode> newInputs = new ArrayList<>();
        boolean changed = false;
        for (RelNode input : node.getInputs()) {
            RelNode normalized = normalizeTree(input);
            newInputs.add(normalized);
            if (normalized != input) changed = true;
        }
        if (changed) {
            if (node instanceof LogicalFilter && !newInputs.isEmpty()) {
                LogicalFilter filter = (LogicalFilter) node;
                RelNode newInput = newInputs.get(0);
                RexNode condition = filter.getCondition();

                // Rebuild condition with correct types
                RexNode fixedCondition = fixRexTypes(condition, newInput);
                node = LogicalFilter.create(newInput, fixedCondition);
            } else {
                node = node.copy(node.getTraitSet(), newInputs);
            }
        }

        // Only normalize Join nodes
        if (!(node instanceof Join)) {
            return node;
        }

        return normalizeJoin((Join) node);
    }

    public static RexNode fixRexTypes(RexNode expr, RelNode input) {
        if (expr instanceof RexInputRef) {
            RexInputRef ref = (RexInputRef) expr;
            int idx = ref.getIndex();
            if (idx < input.getRowType().getFieldCount()) {
                return input.getCluster().getRexBuilder().makeInputRef(
                    input.getRowType().getFieldList().get(idx).getType(), idx);
            }
            return ref;
        }
        if (expr instanceof org.apache.calcite.rex.RexSubQuery) {
            org.apache.calcite.rex.RexSubQuery sub = (org.apache.calcite.rex.RexSubQuery) expr;
            List<RexNode> newOps = new ArrayList<>();
            for (RexNode op : sub.getOperands()) {
                newOps.add(fixRexTypes(op, input));
            }
            return sub.clone(sub.rel).clone(sub.getType(), newOps);
        }
        if (expr instanceof org.apache.calcite.rex.RexCall) {
            org.apache.calcite.rex.RexCall call = (org.apache.calcite.rex.RexCall) expr;
            List<RexNode> newOps = new ArrayList<>();
            for (RexNode op : call.getOperands()) {
                newOps.add(fixRexTypes(op, input));
            }
            return input.getCluster().getRexBuilder().makeCall(call.getOperator(), newOps);
        }
        return expr;
    }

    /**
     * If the right child of this join is also a join, rotate to left-deep.
     * Applied recursively until no right child is a join.
     */
    private RelNode normalizeJoin(Join join) {
        RelNode lhs = unwrap(join.getLeft());
        RelNode rhs = unwrap(join.getRight());

        // Look through identity Projections to find underlying Joins
        RelNode rhsInner = unwrapIdentityProject(rhs);

        // Only rotate if RHS (possibly under an identity Proj) is a join
        if (!(rhsInner instanceof Join)) {
            return join;
        }

        Join rhsJoin = (Join) rhsInner;
        RelNode b = unwrap(rhsJoin.getLeft());
        RelNode c = unwrap(rhsJoin.getRight());

        // Determine which side of rhsJoin contains the keys referenced
        // by join's right-side join condition
        List<ColumnRef> rhsJoinKeys = extractRhsJoinKeys(join);
        List<ColumnRef> bCols = registry.outputColumnsOf(b);
        List<ColumnRef> cCols = registry.outputColumnsOf(c);

        boolean keysInB = false;
        for (ColumnRef key : rhsJoinKeys) {
            // Prefer exact match; use disambiguation-stripping only if exact
            // match fails AND only one side matches after stripping
            if (bCols.contains(key)) {
                keysInB = true;
                break;
            }
            if (!cCols.contains(key)
                    && containsWithDisambiguationStripping(bCols, key)
                    && !containsWithDisambiguationStripping(cCols, key)) {
                keysInB = true;
                break;
            }
        }

        RelNode result;
        if (keysInB) {
            // Pattern 1: join0(A, join1(B, C)) => join1(join0(A, B), C)
            RelNode newInner = rebuildJoin(join, lhs, b);
            if (newInner == null) return null;
            // Recursively normalize newInner: after rotation, it may still be
            // right-deep (e.g., B was itself a multi-way join from a query binding)
            if (newInner instanceof Join) {
                newInner = normalizeJoin((Join) newInner);
                if (newInner == null) return null;
            }
            result = rebuildJoin(rhsJoin, newInner, c);
            if (result == null) return null;
        } else {
            // Pattern 2: join0(A, join1(B, C)) => join1(join0(A, C), B)
            RelNode newInner = rebuildJoin(join, lhs, c);
            if (newInner == null) return null;
            if (newInner instanceof Join) {
                newInner = normalizeJoin((Join) newInner);
                if (newInner == null) return null;
            }
            result = rebuildJoin(rhsJoin, newInner, b);
            if (result == null) return null;
        }

        // Recurse: the result might still have right-deep patterns
        if (result instanceof Join) {
            result = normalizeJoin((Join) result);
        }

        return result;
    }

    /**
     * Extract the ColumnRefs referenced by the join's right-side key
     * (the RexInputRef with index >= leftFieldCount).
     */
    private List<ColumnRef> extractRhsJoinKeys(Join join) {
        List<ColumnRef> result = new ArrayList<>();
        RexNode condition = join.getCondition();
        int leftFieldCount = join.getLeft().getRowType().getFieldCount();
        List<ColumnRef> allCols = registry.outputColumnsOf(join);

        extractRhsKeysFromCondition(condition, leftFieldCount, allCols, result);
        return result;
    }

    private void extractRhsKeysFromCondition(RexNode condition, int leftFieldCount,
                                              List<ColumnRef> allCols, List<ColumnRef> result) {
        if (condition instanceof org.apache.calcite.rex.RexCall) {
            org.apache.calcite.rex.RexCall call = (org.apache.calcite.rex.RexCall) condition;
            String opName = call.getOperator().getName();
            if ("=".equals(opName) && call.getOperands().size() == 2) {
                for (RexNode operand : call.getOperands()) {
                    if (operand instanceof RexInputRef) {
                        int idx = ((RexInputRef) operand).getIndex();
                        if (idx >= leftFieldCount && idx < allCols.size()) {
                            result.add(allCols.get(idx));
                        }
                    }
                }
            } else if ("AND".equals(opName)) {
                for (RexNode operand : call.getOperands()) {
                    extractRhsKeysFromCondition(operand, leftFieldCount, allCols, result);
                }
            }
        }
    }

    /**
     * Rebuild a join with new left and right children, adjusting the
     * join condition's RexInputRef indices using ColumnRefRegistry.
     *
     * <p>Uses the join's combined row type to map each RexInputRef index
     * to a ColumnRef, then resolves each ColumnRef on both new children
     * to determine the correct left/right placement.
     *
     * <p>When ColumnRef-based resolution fails (common with self-joins where
     * disambiguation suffixes like {@code people$47} don't transfer across
     * tree rotations), falls back to positional index mapping based on the
     * known field count ranges of the original join's children.
     */
    private RelNode rebuildJoin(Join originalJoin, RelNode newLeft, RelNode newRight) {
        RexBuilder rexBuilder = newLeft.getCluster().getRexBuilder();

        // Use the join's combined output columns for index → ColumnRef mapping
        List<ColumnRef> origCols = registry.outputColumnsOf(originalJoin);

        // Collect ref pairs from the condition
        List<int[]> refPairs = new ArrayList<>();
        extractRefPairsFromCondition(originalJoin.getCondition(), refPairs);

        if (refPairs.isEmpty()) {
            return LogicalJoin.create(newLeft, newRight, Collections.emptyList(),
                    rexBuilder.makeLiteral(true), Collections.emptySet(),
                    originalJoin.getJoinType());
        }

        // Resolve keys in new children
        List<RexNode> equalities = new ArrayList<>();
        int newLeftCount = newLeft.getRowType().getFieldCount();

        // Build index mapping from original join fields → new left/right fields.
        // The original join has origLeft + origRight fields. We know which original
        // subtrees ended up in the new left vs new right. Use ColumnRef-based
        // resolution first, then fall back to positional matching with
        // disambiguation-stripping for self-join scenarios.
        List<ColumnRef> newLeftCols = registry.outputColumnsOf(newLeft);
        List<ColumnRef> newRightCols = registry.outputColumnsOf(newRight);

        for (int[] pair : refPairs) {
            int idx0 = pair[0];
            int idx1 = pair[1];
            if (idx0 >= origCols.size() || idx1 >= origCols.size()) continue;

            ColumnRef ref0 = origCols.get(idx0);
            ColumnRef ref1 = origCols.get(idx1);

            // Try resolving each ref on both sides
            int leftIdx0 = registry.resolveIndex(ref0, newLeft);
            int rightIdx0 = registry.resolveIndex(ref0, newRight);
            int leftIdx1 = registry.resolveIndex(ref1, newLeft);
            int rightIdx1 = registry.resolveIndex(ref1, newRight);

            // If ColumnRef resolution fails for a ref (e.g., due to disambiguation
            // suffix mismatch in self-join scenarios), try stripping the $N suffix
            // and matching by base table name + column name.
            // Only apply fallback when the OTHER ref already resolved to one specific
            // side, and this ref needs to resolve to the opposite side. This prevents
            // incorrectly matching a disambiguated ref to the wrong table instance
            // when the same table appears multiple times.
            if (leftIdx0 < 0 && rightIdx0 < 0) {
                // ref0 unresolved — try stripping, but prefer the side opposite to ref1
                if (leftIdx1 >= 0 && rightIdx1 < 0) {
                    // ref1 is on left, so ref0 should be on right
                    rightIdx0 = resolveWithDisambiguationStripping(ref0, newRightCols);
                } else if (rightIdx1 >= 0 && leftIdx1 < 0) {
                    // ref1 is on right, so ref0 should be on left
                    leftIdx0 = resolveWithDisambiguationStripping(ref0, newLeftCols);
                }
                // If ref1 is also unresolved, don't guess — skip this pair
            }
            if (leftIdx1 < 0 && rightIdx1 < 0) {
                // ref1 unresolved — try stripping, prefer opposite side to ref0
                if (leftIdx0 >= 0 && rightIdx0 < 0) {
                    // ref0 is on left, so ref1 should be on right
                    rightIdx1 = resolveWithDisambiguationStripping(ref1, newRightCols);
                } else if (rightIdx0 >= 0 && leftIdx0 < 0) {
                    // ref0 is on right, so ref1 should be on left
                    leftIdx1 = resolveWithDisambiguationStripping(ref1, newLeftCols);
                }
            }

            int newLeftIdx = -1;
            int newRightIdx = -1;

            if (leftIdx0 >= 0 && rightIdx1 >= 0) {
                newLeftIdx = leftIdx0;
                newRightIdx = rightIdx1;
            } else if (leftIdx1 >= 0 && rightIdx0 >= 0) {
                newLeftIdx = leftIdx1;
                newRightIdx = rightIdx0;
            }

            if (newLeftIdx >= 0 && newRightIdx >= 0) {
                RexNode lhsRef = rexBuilder.makeInputRef(
                        newLeft.getRowType().getFieldList().get(newLeftIdx).getType(), newLeftIdx);
                RexNode rhsRef = rexBuilder.makeInputRef(
                        newRight.getRowType().getFieldList().get(newRightIdx).getType(),
                        newLeftCount + newRightIdx);
                equalities.add(rexBuilder.makeCall(SqlStdOperatorTable.EQUALS, lhsRef, rhsRef));
            }
        }

        RexNode newCondition;
        if (!refPairs.isEmpty() && equalities.isEmpty()) {
            return null;
        } else if (equalities.size() == 1) {
            newCondition = equalities.get(0);
        } else {
            newCondition = rexBuilder.makeCall(SqlStdOperatorTable.AND, equalities);
        }

        return LogicalJoin.create(newLeft, newRight, Collections.emptyList(),
                newCondition, Collections.emptySet(), originalJoin.getJoinType());
    }

    /**
     * Resolve a ColumnRef by stripping disambiguation suffixes from both the
     * reference and the target columns, then matching by base table name + column name.
     *
     * <p>Unlike {@link ColumnRefRegistry#resolveIndex}, this method will match
     * disambiguated refs (e.g., {@code people$47.id}) against disambiguated targets
     * (e.g., {@code people$22.id}) as long as the base table names match. This is
     * necessary during join rotation because the positional {@code $N} suffix from
     * the original join tree no longer corresponds to positions in the new tree.
     *
     * @return the index in {@code cols} of the first match, or -1 if not found
     */
    private static int resolveWithDisambiguationStripping(ColumnRef ref, List<ColumnRef> cols) {
        String baseTable = stripDisambiguationSuffix(ref.getTableName());
        String colName = ref.getColumnName();

        for (int i = 0; i < cols.size(); i++) {
            ColumnRef candidate = cols.get(i);
            String candidateBase = stripDisambiguationSuffix(candidate.getTableName());
            // Skip expression placeholders
            if (candidateBase.startsWith("$")) continue;
            if (candidateBase.equals(baseTable)
                    && candidate.getColumnName().equalsIgnoreCase(colName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Check if a list of ColumnRefs contains a match for the given ref after
     * stripping disambiguation suffixes from both sides.
     */
    private static boolean containsWithDisambiguationStripping(List<ColumnRef> cols, ColumnRef ref) {
        return resolveWithDisambiguationStripping(ref, cols) >= 0;
    }

    /**
     * Strip the {@code $N} disambiguation suffix from a table name.
     * E.g., {@code "contacts$41"} → {@code "contacts"}.
     */
    private static String stripDisambiguationSuffix(String tableName) {
        int dollarIdx = tableName.indexOf('$');
        return dollarIdx >= 0 ? tableName.substring(0, dollarIdx) : tableName;
    }

    /**
     * Extract all RexInputRef index pairs from equi-join conditions,
     * without assuming which side they belong to.
     */
    private void extractRefPairsFromCondition(RexNode condition, List<int[]> pairs) {
        if (condition instanceof org.apache.calcite.rex.RexCall) {
            org.apache.calcite.rex.RexCall call = (org.apache.calcite.rex.RexCall) condition;
            String opName = call.getOperator().getName();
            if ("=".equals(opName) && call.getOperands().size() == 2) {
                RexNode left = call.getOperands().get(0);
                RexNode right = call.getOperands().get(1);
                if (left instanceof RexInputRef && right instanceof RexInputRef) {
                    pairs.add(new int[]{
                            ((RexInputRef) left).getIndex(),
                            ((RexInputRef) right).getIndex()});
                }
            } else if ("AND".equals(opName)) {
                for (RexNode operand : call.getOperands()) {
                    extractRefPairsFromCondition(operand, pairs);
                }
            }
        }
    }

    private static RelNode unwrap(RelNode node) {
        while (node instanceof HepRelVertex) {
            node = ((HepRelVertex) node).getCurrentRel();
        }
        // Handle VolcanoPlanner's RelSubset
        if (node instanceof org.apache.calcite.plan.volcano.RelSubset) {
            org.apache.calcite.plan.volcano.RelSubset subset =
                    (org.apache.calcite.plan.volcano.RelSubset) node;
            RelNode original = subset.getOriginal();
            if (original != null) return original;
        }
        return node;
    }

    /**
     * Unwrap identity Projections to find the underlying node.
     * Identity projections are pass-through (all columns in order, no transformation).
     */
    private static RelNode unwrapIdentityProject(RelNode node) {
        node = unwrap(node);
        while (node instanceof org.apache.calcite.rel.logical.LogicalProject) {
            org.apache.calcite.rel.logical.LogicalProject proj =
                    (org.apache.calcite.rel.logical.LogicalProject) node;
            boolean isIdentity = true;
            if (proj.getProjects().size() != proj.getInput().getRowType().getFieldCount()) break;
            for (int i = 0; i < proj.getProjects().size(); i++) {
                RexNode expr = proj.getProjects().get(i);
                if (!(expr instanceof RexInputRef) || ((RexInputRef) expr).getIndex() != i) {
                    isIdentity = false;
                    break;
                }
            }
            if (!isIdentity) break;
            node = unwrap(proj.getInput());
        }
        return node;
    }
}
