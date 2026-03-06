package org.autorewriter.rewriter.rule.symbol;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalAggregate;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rel.logical.LogicalJoin;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rel.logical.LogicalTableScan;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexSubQuery;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts {@link Symbol} placeholders from a Calcite template {@link RelNode} tree.
 *
 * <p>The extractor walks the tree and looks for names that match the pattern {@code [tasp]\d+}
 * in the positions where template placeholders are expected:
 * <ul>
 *   <li>{@link LogicalTableScan} — table name ({@code t\d+})</li>
 *   <li>{@link LogicalProject} — field names ({@code a\d+}, {@code s\d+})</li>
 *   <li>{@link LogicalFilter} — condition RexNode operator names ({@code p\d+}),
 *       field names ({@code a\d+})</li>
 *   <li>{@link LogicalJoin} — condition RexNode field names ({@code a\d+})</li>
 *   <li>{@link LogicalAggregate} — field names from group-by columns ({@code a\d+})</li>
 * </ul>
 *
 * <p>The result is a {@link Map} from placeholder name to {@link Symbol}, preserving
 * insertion (discovery) order.
 */
public final class SymbolExtractor {

    private SymbolExtractor() {
        // utility class
    }

    /**
     * Extract all symbols from the given template {@link RelNode} tree.
     *
     * @param templateRoot the root of the template RelNode tree
     * @return an unmodifiable map from placeholder name to Symbol, in discovery order
     */
    public static Map<String, Symbol> extract(RelNode templateRoot) {
        Map<String, Symbol> symbols = new LinkedHashMap<>();
        walkRelNode(templateRoot, symbols);
        return Collections.unmodifiableMap(symbols);
    }

    // ── RelNode dispatch ────────────────────────────────────────────────

    private static void walkRelNode(RelNode node, Map<String, Symbol> symbols) {
        if (node instanceof LogicalTableScan) {
            extractFromTableScan((LogicalTableScan) node, symbols);
        } else if (node instanceof LogicalProject) {
            extractFromProject((LogicalProject) node, symbols);
        } else if (node instanceof LogicalFilter) {
            extractFromFilter((LogicalFilter) node, symbols);
        } else if (node instanceof LogicalJoin) {
            extractFromJoin((LogicalJoin) node, symbols);
        } else if (node instanceof LogicalAggregate) {
            extractFromAggregate((LogicalAggregate) node, symbols);
        }

        // Recurse into inputs (children)
        for (RelNode input : node.getInputs()) {
            walkRelNode(input, symbols);
        }
    }

    // ── Per-operator extraction ─────────────────────────────────────────

    private static void extractFromTableScan(LogicalTableScan scan, Map<String, Symbol> symbols) {
        List<String> qualifiedName = scan.getTable().getQualifiedName();
        // The placeholder is the last segment of the qualified name (typically just the table name)
        String tableName = qualifiedName.get(qualifiedName.size() - 1);
        tryAdd(tableName, symbols);
    }

    private static void extractFromProject(LogicalProject project, Map<String, Symbol> symbols) {
        // Check output field names for a\d+ or s\d+ placeholders
        for (String fieldName : project.getRowType().getFieldNames()) {
            tryAdd(fieldName, symbols);
        }
        // Walk projection expressions for predicate / attr placeholders
        for (RexNode expr : project.getProjects()) {
            walkRexNode(expr, symbols);
        }
    }

    private static void extractFromFilter(LogicalFilter filter, Map<String, Symbol> symbols) {
        // Check output field names for a\d+ placeholders
        for (String fieldName : filter.getRowType().getFieldNames()) {
            tryAdd(fieldName, symbols);
        }
        // Walk condition for p\d+ and a\d+ placeholders
        walkRexNode(filter.getCondition(), symbols);
    }

    private static void extractFromJoin(LogicalJoin join, Map<String, Symbol> symbols) {
        // Check output field names for a\d+ placeholders
        for (String fieldName : join.getRowType().getFieldNames()) {
            tryAdd(fieldName, symbols);
        }
        // Walk join condition for p\d+ and a\d+ placeholders
        walkRexNode(join.getCondition(), symbols);
    }

    private static void extractFromAggregate(LogicalAggregate agg, Map<String, Symbol> symbols) {
        // Check output field names for a\d+ placeholders
        for (String fieldName : agg.getRowType().getFieldNames()) {
            tryAdd(fieldName, symbols);
        }
    }

    // ── RexNode walk ────────────────────────────────────────────────────

    private static void walkRexNode(RexNode node, Map<String, Symbol> symbols) {
        if (node instanceof RexCall) {
            RexCall call = (RexCall) node;
            // Operator name may be a predicate placeholder like p0
            String opName = call.getOperator().getName();
            tryAdd(opName, symbols);
            // Recurse into operands
            for (RexNode operand : call.getOperands()) {
                walkRexNode(operand, symbols);
            }
        } else if (node instanceof RexSubQuery) {
            RexSubQuery subQuery = (RexSubQuery) node;
            for (RexNode operand : subQuery.getOperands()) {
                walkRexNode(operand, symbols);
            }
            // Recurse into the sub-query RelNode tree
            walkRelNode(subQuery.rel, symbols);
        }
        // RexInputRef, RexLiteral, etc. don't carry placeholder names directly
    }

    // ── Helper ──────────────────────────────────────────────────────────

    private static void tryAdd(String name, Map<String, Symbol> symbols) {
        if (SymbolKind.isSymbolName(name)) {
            symbols.putIfAbsent(name, Symbol.of(name));
        }
    }
}
