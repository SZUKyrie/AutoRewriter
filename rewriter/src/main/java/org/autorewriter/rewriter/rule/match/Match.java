package org.autorewriter.rewriter.rule.match;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.plan.volcano.RelSubset;import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.*;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rex.*;
import org.autorewriter.rewriter.rule.model.Model;
import org.autorewriter.rewriter.rule.symbol.*;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.autorewriter.rewriter.rule.util.ColumnRefResolver;

import java.util.*;

/**
 * Main recursive matching engine.
 *
 * <p>Matches a source template {@link RelNode} against a query {@link RelNode},
 * binding symbols in the {@link Model}. This is the WeTune-style matching
 * algorithm adapted for Calcite.
 *
 * <p>Template placeholder conventions (from ShardingSphere parser):
 * <ul>
 *   <li>{@code t\d+} in LogicalTableScan table name &rarr; TABLE symbol (wildcard for any subtree)</li>
 *   <li>{@code a\d+} in field names &rarr; ATTRS symbol (column references)</li>
 *   <li>{@code p\d+} in RexCall operator name &rarr; PRED symbol (predicate expression)</li>
 *   <li>{@code s\d+} in field names &rarr; SCHEMA symbol (output schema)</li>
 * </ul>
 */
@Slf4j
public class Match {

    /**
     * Match a source template against a query RelNode, binding symbols in the model.
     * Returns true if matching succeeds.
     */
    public static boolean match(RelNode template, RelNode query, Model model) {
        query = unwrapHepVertex(query);

        // Handle VolcanoPlanner's RelSubset: try matching against each alternative
        // in the equivalence set. A RelSubset may contain multiple equivalent nodes
        // (e.g., LogicalInSubFilter AND LogicalFilter(IN)), and we need to find one
        // that structurally matches the template.
        if (query instanceof RelSubset) {
            RelSubset subset = (RelSubset) query;
            for (RelNode rel : subset.getRels()) {
                if (rel instanceof RelSubset) continue;
                Model derived = model.derive();
                if (match(template, rel, derived)) {
                    // Found a matching alternative — replay into the real model
                    return match(template, rel, model);
                }
            }
            return false;
        }

        // 1. If template is a LogicalTableScan with placeholder name (t\d+) = INPUT
        //    It can match ANY query RelNode subtree (wildcard)
        if (template instanceof LogicalTableScan) {
            String tableName = getTableName((LogicalTableScan) template);
            if (SymbolKind.isSymbolName(tableName) && tableName.charAt(0) == 't') {
                Symbol sym = Symbol.of(tableName);
                if (!model.assign(sym, query)) return false;
                return model.checkConstraints();
            }
            // Not a placeholder - exact match
            if (!(query instanceof LogicalTableScan)) return false;
            return tableName.equals(getTableName((LogicalTableScan) query));
        }

        // 2. LogicalAggregate (PROJ* / DISTINCT)
        if (template instanceof LogicalAggregate) {
            if (!(query instanceof LogicalAggregate)) return false;
            return matchAggregate((LogicalAggregate) template, (LogicalAggregate) query, model);
        }

        // 3. LogicalProject (PROJ)
        if (template instanceof LogicalProject) {
            if (!(query instanceof LogicalProject)) return false;
            return matchProject((LogicalProject) template, (LogicalProject) query, model);
        }

        // 4. LogicalFilter
        if (template instanceof LogicalFilter) {
            if (!(query instanceof LogicalFilter)) return false;
            return matchFilter((LogicalFilter) template, (LogicalFilter) query, model);
        }

        // 5. LogicalJoin
        if (template instanceof LogicalJoin) {
            if (!(query instanceof LogicalJoin)) return false;
            return matchJoin((LogicalJoin) template, (LogicalJoin) query, model);
        }

        // Default: class must match, then recurse on children
        if (!template.getClass().equals(query.getClass())) return false;
        if (template.getInputs().size() != query.getInputs().size()) return false;
        for (int i = 0; i < template.getInputs().size(); i++) {
            if (!match(template.getInput(i), query.getInput(i), model)) return false;
        }
        return true;
    }

    // ── Project matching ──────────────────────────────────────────────────

    private static boolean matchProject(LogicalProject template, LogicalProject query, Model model) {
        // First recurse on child
        if (!match(template.getInput(), query.getInput(), model)) return false;

        // Extract placeholder names from template field names
        List<String> templateFields = template.getRowType().getFieldNames();

        // Find attrs placeholder (a\d+) and schema placeholder (s\d+)
        String attrsPlaceholder = null;
        String schemaPlaceholder = null;
        for (String field : templateFields) {
            if (SymbolKind.isSymbolName(field)) {
                if (field.charAt(0) == 'a') attrsPlaceholder = field;
                else if (field.charAt(0) == 's') schemaPlaceholder = field;
            }
        }

        // Bind attrs: resolve all query projection columns to ColumnRefs
        if (attrsPlaceholder != null) {
            Symbol attrsSym = Symbol.of(attrsPlaceholder);
            List<ColumnRef> columnRefs = new ArrayList<>();
            RelNode queryInput = unwrapHepVertex(query.getInput());
            for (int i = 0; i < query.getProjects().size(); i++) {
                RexNode project = query.getProjects().get(i);
                if (project instanceof RexInputRef) {
                    int idx = ((RexInputRef) project).getIndex();
                    ColumnRef ref = ColumnRefResolver.resolve(idx, queryInput);
                    columnRefs.add(ref);
                } else {
                    // For complex expressions, use the output field name
                    String fieldName = query.getRowType().getFieldNames().get(i);
                    columnRefs.add(new ColumnRef("$expr", fieldName));
                }
            }
            if (!model.assign(attrsSym, columnRefs)) return false;

            // Also store the projection RexNodes for later use in Instantiation
            model.putExtra(attrsPlaceholder + "_projects", new ArrayList<>(query.getProjects()));
        }

        // Bind schema
        if (schemaPlaceholder != null) {
            Symbol schemaSym = Symbol.of(schemaPlaceholder);
            if (!model.assign(schemaSym, query.getRowType())) return false;
        }

        return model.checkConstraints();
    }

    // ── Aggregate matching (DISTINCT / Proj*) ─────────────────────────────

    private static boolean matchAggregate(LogicalAggregate template, LogicalAggregate query, Model model) {
        // Both must be DISTINCT (empty agg call lists)
        if (!template.getAggCallList().isEmpty() || !query.getAggCallList().isEmpty()) return false;

        // Recurse on child
        if (!match(template.getInput(), query.getInput(), model)) return false;

        // Extract attrs placeholder from template field names
        List<String> templateFields = template.getRowType().getFieldNames();
        for (String field : templateFields) {
            if (SymbolKind.isSymbolName(field) && field.charAt(0) == 'a') {
                Symbol attrsSym = Symbol.of(field);
                // Resolve query's group-by columns to ColumnRefs
                RelNode queryInput = unwrapHepVertex(query.getInput());
                List<ColumnRef> columnRefs = new ArrayList<>();
                for (int idx : query.getGroupSet()) {
                    ColumnRef ref = ColumnRefResolver.resolve(idx, queryInput);
                    columnRefs.add(ref);
                }
                if (!model.assign(attrsSym, columnRefs)) return false;
                break; // only one attrs placeholder expected
            }
        }

        // Schema placeholder
        for (String field : templateFields) {
            if (SymbolKind.isSymbolName(field) && field.charAt(0) == 's') {
                Symbol schemaSym = Symbol.of(field);
                if (!model.assign(schemaSym, query.getRowType())) return false;
                break;
            }
        }

        return model.checkConstraints();
    }

    // ── Filter matching ───────────────────────────────────────────────────

    private static boolean matchFilter(LogicalFilter template, LogicalFilter query, Model model) {
        RexNode templateCond = template.getCondition();

        // Check if this is an InSubFilter (template condition contains RexSubQuery)
        if (hasRexSubQuery(templateCond)) {
            return matchInSubFilter(template, query, model);
        }

        // Use filter chain matching when EITHER the template OR the query has
        // multiple consecutive filters. This handles the common case where
        // FilterSplitter produces a chain of filters (e.g., Filter → Filter →
        // Filter → Join) but the template has only a single filter.
        RelNode templateInput = unwrapHepVertex(template.getInput());
        RelNode queryInput = unwrapHepVertex(query.getInput());
        if (templateInput instanceof LogicalFilter || queryInput instanceof LogicalFilter) {
            return FilterMatcher.matchFilterChain(template, query, model);
        }

        // Simple single filter matching
        // Recurse on child first
        if (!match(template.getInput(), query.getInput(), model)) return false;

        // Match the filter condition
        if (!matchRexNode(templateCond, query.getCondition(), query, model)) return false;

        return model.checkConstraints();
    }

    // ── InSubFilter matching ──────────────────────────────────────────────

    private static boolean matchInSubFilter(LogicalFilter template, LogicalFilter query, Model model) {
        // Extract RexSubQuery from both conditions
        RexSubQuery templateSubQuery = extractRexSubQuery(template.getCondition());
        if (templateSubQuery == null) return false;

        RexSubQuery querySubQuery = extractRexSubQuery(query.getCondition());
        if (querySubQuery == null) return false;

        // Match main input
        if (!match(template.getInput(), query.getInput(), model)) return false;

        // Match subquery RelNode
        if (!match(templateSubQuery.rel, querySubQuery.rel, model)) return false;

        // Bind attrs for the correlation columns from template's operands
        for (RexNode operand : templateSubQuery.operands) {
            if (operand instanceof RexInputRef) {
                int templateIdx = ((RexInputRef) operand).getIndex();
                List<String> inputFieldNames = template.getInput().getRowType().getFieldNames();
                if (templateIdx < inputFieldNames.size()) {
                    String fieldName = inputFieldNames.get(templateIdx);
                    if (SymbolKind.isSymbolName(fieldName) && fieldName.charAt(0) == 'a') {
                        Symbol attrsSym = Symbol.of(fieldName);
                        List<ColumnRef> columnRefs = new ArrayList<>();
                        for (RexNode qOp : querySubQuery.operands) {
                            if (qOp instanceof RexInputRef) {
                                int qIdx = ((RexInputRef) qOp).getIndex();
                                ColumnRef ref = ColumnRefResolver.resolve(qIdx, unwrapHepVertex(query.getInput()));
                                columnRefs.add(ref);
                            }
                        }
                        if (!model.assign(attrsSym, columnRefs)) return false;
                    }
                }
            }
        }

        return model.checkConstraints();
    }

    // ── Join matching ─────────────────────────────────────────────────────

    private static boolean matchJoin(LogicalJoin template, LogicalJoin query, Model model) {
        // Check join type
        if (template.getJoinType() != query.getJoinType()) return false;

        // Try direct match first (left-to-left, right-to-right)
        Model directModel = model.derive();
        if (matchJoinWithSides(template, query, directModel, false)) {
            // Replay successful assignments into the parent model
            return matchJoinWithSides(template, query, model, false);
        }

        // For INNER JOIN, try flipped match (left-to-right, right-to-left)
        if (template.getJoinType() == JoinRelType.INNER) {
            Model flippedModel = model.derive();
            if (matchJoinWithSides(template, query, flippedModel, true)) {
                return matchJoinWithSides(template, query, model, true);
            }
        }

        return false;
    }

    private static boolean matchJoinWithSides(LogicalJoin template, LogicalJoin query,
                                               Model model, boolean flipped) {
        RelNode tLeft = template.getLeft();
        RelNode tRight = template.getRight();
        RelNode qLeft = flipped ? query.getRight() : query.getLeft();
        RelNode qRight = flipped ? query.getLeft() : query.getRight();

        if (!match(tLeft, qLeft, model)) return false;
        if (!match(tRight, qRight, model)) return false;

        // Match join condition
        if (!matchJoinCondition(template, query, model, flipped)) return false;

        return model.checkConstraints();
    }

    private static boolean matchJoinCondition(LogicalJoin template, LogicalJoin query, Model model, boolean flipped) {
        RexNode templateCond = template.getCondition();
        RexNode queryCond = query.getCondition();

        int tLeftFieldCount = template.getLeft().getRowType().getFieldCount();
        int qLeftFieldCount = query.getLeft().getRowType().getFieldCount();

        // Extract equi-join key pairs from template and query conditions
        List<int[]> templatePairs = extractJoinKeyPairs(templateCond, tLeftFieldCount);
        List<int[]> queryPairs = extractJoinKeyPairs(queryCond, qLeftFieldCount);

        if (templatePairs.isEmpty() || queryPairs.isEmpty()) {
            // Fallback: try matching via RexNode matching
            return matchRexNode(templateCond, queryCond, query, model);
        }

        // For each template pair, find a matching query pair and bind attrs
        if (templatePairs.size() > queryPairs.size()) return false;

        for (int ti = 0; ti < templatePairs.size(); ti++) {
            int[] tPair = templatePairs.get(ti);
            if (ti >= queryPairs.size()) return false;
            int[] qPair = queryPairs.get(ti);

            int tLeftIdx = tPair[0];
            int tRightIdx = tPair[1] - tLeftFieldCount;

            // Get placeholder names from template's children's field names
            List<String> tLeftFields = template.getLeft().getRowType().getFieldNames();
            List<String> tRightFields = template.getRight().getRowType().getFieldNames();

            String leftField = tLeftIdx < tLeftFields.size() ? tLeftFields.get(tLeftIdx) : null;
            String rightField = tRightIdx < tRightFields.size() ? tRightFields.get(tRightIdx) : null;

            // Get actual column refs from query
            int qLeftIdx = flipped ? (qPair[1] - qLeftFieldCount) : qPair[0];
            int qRightIdx = flipped ? qPair[0] : (qPair[1] - qLeftFieldCount);

            RelNode qLeftChild = unwrapHepVertex(flipped ? query.getRight() : query.getLeft());
            RelNode qRightChild = unwrapHepVertex(flipped ? query.getLeft() : query.getRight());

            if (leftField != null && SymbolKind.isSymbolName(leftField) && leftField.charAt(0) == 'a') {
                Symbol sym = Symbol.of(leftField);
                ColumnRef ref = ColumnRefResolver.resolve(qLeftIdx, qLeftChild);
                if (!model.assign(sym, Collections.singletonList(ref))) return false;
            }

            if (rightField != null && SymbolKind.isSymbolName(rightField) && rightField.charAt(0) == 'a') {
                Symbol sym = Symbol.of(rightField);
                ColumnRef ref = ColumnRefResolver.resolve(qRightIdx, qRightChild);
                if (!model.assign(sym, Collections.singletonList(ref))) return false;
            }
        }

        return true;
    }

    // ── RexNode matching ──────────────────────────────────────────────────

    /**
     * Match a template RexNode against a query RexNode, binding symbols.
     * The {@code queryOperator} provides context for resolving column references.
     */
    static boolean matchRexNode(RexNode template, RexNode query, RelNode queryOperator, Model model) {
        if (template instanceof RexCall && query instanceof RexCall) {
            RexCall tCall = (RexCall) template;
            RexCall qCall = (RexCall) query;

            // Check if template operator is a predicate placeholder (p\d+)
            String opName = tCall.getOperator().getName();
            if (SymbolKind.isSymbolName(opName) && opName.charAt(0) == 'p') {
                Symbol predSym = Symbol.of(opName);
                if (!model.assign(predSym, query)) return false;

                // Collect all column refs from the query predicate and bind attrs
                RelNode resolveTarget = getFilterInput(queryOperator);
                List<ColumnRef> columnRefs = collectColumnRefs(query, resolveTarget);

                // Find the attrs placeholder associated with this predicate
                // from the template's operands (RexInputRef pointing to parent fields)
                for (RexNode operand : tCall.getOperands()) {
                    if (operand instanceof RexInputRef) {
                        int fieldIdx = ((RexInputRef) operand).getIndex();
                        // Look up the field name in the operator's input (for filter, that's filter.getInput())
                        List<String> fieldNames = resolveTarget.getRowType().getFieldNames();
                        if (fieldIdx < fieldNames.size()) {
                            String fieldName = fieldNames.get(fieldIdx);
                            if (SymbolKind.isSymbolName(fieldName) && fieldName.charAt(0) == 'a') {
                                Symbol attrsSym = Symbol.of(fieldName);
                                if (!model.assign(attrsSym, columnRefs)) return false;
                            }
                        }
                    }
                }
                return true;
            }

            // Normal RexCall: operator and operand count must match
            if (!tCall.getOperator().equals(qCall.getOperator())) return false;
            if (tCall.getOperands().size() != qCall.getOperands().size()) return false;
            for (int i = 0; i < tCall.getOperands().size(); i++) {
                if (!matchRexNode(tCall.getOperands().get(i), qCall.getOperands().get(i), queryOperator, model)) {
                    return false;
                }
            }
            return true;
        }

        if (template instanceof RexSubQuery && query instanceof RexSubQuery) {
            RexSubQuery tSub = (RexSubQuery) template;
            RexSubQuery qSub = (RexSubQuery) query;

            if (!tSub.getOperator().equals(qSub.getOperator())) return false;

            if (tSub.getOperands().size() != qSub.getOperands().size()) return false;
            for (int i = 0; i < tSub.getOperands().size(); i++) {
                if (!matchRexNode(tSub.getOperands().get(i), qSub.getOperands().get(i), queryOperator, model)) {
                    return false;
                }
            }
            // Recurse into the sub-query RelNode
            return match(tSub.rel, qSub.rel, model);
        }

        if (template instanceof RexInputRef && query instanceof RexInputRef) {
            // InputRef matching always succeeds; binding happens through parent
            return true;
        }

        if (template instanceof RexLiteral && query instanceof RexLiteral) {
            return template.equals(query);
        }

        // Fallback: string comparison
        return template.toString().equals(query.toString());
    }

    // ── Utility methods ───────────────────────────────────────────────────

    /**
     * Unwrap planner-internal wrappers to get the real underlying RelNode.
     * Handles HepPlanner's HepRelVertex. For VolcanoPlanner's RelSubset,
     * see the handling in {@link #match} which iterates alternatives.
     */
    public static RelNode unwrapHepVertex(RelNode node) {
        while (node instanceof HepRelVertex) {
            node = ((HepRelVertex) node).getCurrentRel();
        }
        // For RelSubset, return the original logical node as a best-effort
        // default. Match.match() handles full iteration when needed.
        if (node instanceof RelSubset) {
            RelSubset subset = (RelSubset) node;
            RelNode original = subset.getOriginal();
            if (original != null) {
                return original;
            }
        }
        return node;
    }

    private static String getTableName(LogicalTableScan scan) {
        List<String> names = scan.getTable().getQualifiedName();
        return names.get(names.size() - 1);
    }

    private static boolean hasRexSubQuery(RexNode node) {
        if (node instanceof RexSubQuery) return true;
        if (node instanceof RexCall) {
            for (RexNode operand : ((RexCall) node).getOperands()) {
                if (hasRexSubQuery(operand)) return true;
            }
        }
        return false;
    }

    private static RexSubQuery extractRexSubQuery(RexNode node) {
        if (node instanceof RexSubQuery) return (RexSubQuery) node;
        if (node instanceof RexCall) {
            for (RexNode operand : ((RexCall) node).getOperands()) {
                RexSubQuery sub = extractRexSubQuery(operand);
                if (sub != null) return sub;
            }
        }
        return null;
    }

    /**
     * Extract equi-join key pairs from a join condition.
     * Each pair is [leftIdx, rightIdx] where rightIdx includes the leftFieldCount offset.
     */
    private static List<int[]> extractJoinKeyPairs(RexNode condition, int leftFieldCount) {
        List<int[]> pairs = new ArrayList<>();
        extractJoinKeyPairsRecursive(condition, leftFieldCount, pairs);
        return pairs;
    }

    private static void extractJoinKeyPairsRecursive(RexNode condition, int leftFieldCount, List<int[]> pairs) {
        if (condition instanceof RexCall) {
            RexCall call = (RexCall) condition;
            String opName = call.getOperator().getName();
            if ("=".equals(opName) && call.getOperands().size() == 2) {
                RexNode left = call.getOperands().get(0);
                RexNode right = call.getOperands().get(1);
                if (left instanceof RexInputRef && right instanceof RexInputRef) {
                    int leftIdx = ((RexInputRef) left).getIndex();
                    int rightIdx = ((RexInputRef) right).getIndex();
                    // Normalize: left from LHS, right from RHS
                    if (leftIdx < leftFieldCount && rightIdx >= leftFieldCount) {
                        pairs.add(new int[]{leftIdx, rightIdx});
                    } else if (rightIdx < leftFieldCount && leftIdx >= leftFieldCount) {
                        pairs.add(new int[]{rightIdx, leftIdx});
                    }
                }
            } else if ("AND".equals(opName)) {
                for (RexNode operand : call.getOperands()) {
                    extractJoinKeyPairsRecursive(operand, leftFieldCount, pairs);
                }
            }
        }
    }

    /**
     * Collect all column references from a RexNode expression tree.
     */
    private static List<ColumnRef> collectColumnRefs(RexNode expr, RelNode resolveTarget) {
        List<ColumnRef> refs = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        collectColumnRefsRecursive(expr, resolveTarget, refs, seen);
        return refs;
    }

    private static void collectColumnRefsRecursive(RexNode expr, RelNode resolveTarget,
                                                    List<ColumnRef> refs, Set<Integer> seen) {
        if (expr instanceof RexInputRef) {
            int idx = ((RexInputRef) expr).getIndex();
            if (seen.add(idx)) {
                ColumnRef ref = ColumnRefResolver.resolve(idx, resolveTarget);
                refs.add(ref);
            }
        } else if (expr instanceof RexCall) {
            for (RexNode operand : ((RexCall) expr).getOperands()) {
                collectColumnRefsRecursive(operand, resolveTarget, refs, seen);
            }
        } else if (expr instanceof RexSubQuery) {
            for (RexNode operand : ((RexSubQuery) expr).getOperands()) {
                collectColumnRefsRecursive(operand, resolveTarget, refs, seen);
            }
        }
    }

    /**
     * For a filter operator, returns its input (the node whose row type the condition references).
     * For other operators, returns the operator itself.
     */
    private static RelNode getFilterInput(RelNode operator) {
        RelNode unwrapped = unwrapHepVertex(operator);
        if (unwrapped instanceof LogicalFilter) {
            return unwrapHepVertex(((LogicalFilter) unwrapped).getInput());
        }
        return unwrapped;
    }
}
