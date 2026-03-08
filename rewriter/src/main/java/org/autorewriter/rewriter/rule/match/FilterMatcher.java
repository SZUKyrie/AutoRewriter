package org.autorewriter.rewriter.rule.match;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexSubQuery;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.LogicalInSubFilter;
import org.autorewriter.rewriter.rule.model.Model;
import org.autorewriter.rewriter.rule.symbol.SymbolKind;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles filter chain combinatorial matching.
 *
 * <p>Follows WeTune's approach: all filter-like nodes (LogicalFilter and
 * LogicalInSubFilter) form a single chain. Template filters are assigned to
 * query filters by type:
 * <ul>
 *   <li>Simple template filter (no RexSubQuery) matches only LogicalFilter</li>
 *   <li>InSub template filter (has RexSubQuery) matches only LogicalInSubFilter</li>
 * </ul>
 */
@Slf4j
public class FilterMatcher {

    /**
     * Entry point when both template and query heads are LogicalFilter.
     */
    public static boolean matchFilterChain(LogicalFilter templateHead, LogicalFilter queryHead, Model model) {
        List<RelNode> templateChain = collectGeneralizedFilterChain(templateHead);
        List<RelNode> queryChain = collectGeneralizedFilterChain(queryHead);

        RelNode templateBottom = getGeneralizedChainInput(templateHead);
        RelNode queryBottom = getGeneralizedChainInput(queryHead);

        return doMatchFilterChain(templateChain, queryChain, templateBottom, queryBottom, queryHead, model);
    }

    /**
     * Entry point when query head is LogicalInSubFilter (template is LogicalFilter).
     */
    public static boolean matchFilterChainFromInSub(LogicalFilter templateHead, LogicalInSubFilter queryHead, Model model) {
        List<RelNode> templateChain = collectGeneralizedFilterChain(templateHead);
        List<RelNode> queryChain = collectGeneralizedFilterChain(queryHead);

        RelNode templateBottom = getGeneralizedChainInput(templateHead);
        RelNode queryBottom = getGeneralizedChainInput(queryHead);

        return doMatchFilterChain(templateChain, queryChain, templateBottom, queryBottom, queryHead, model);
    }

    private static boolean doMatchFilterChain(List<RelNode> templateChain, List<RelNode> queryChain,
                                               RelNode templateBottom, RelNode queryBottom,
                                               RelNode queryOperator, Model model) {
        // Count template simple filters (non-InSub) and InSub filters separately
        long templateSimpleCount = templateChain.stream().filter(n -> n instanceof LogicalFilter).count();
        long querySimpleCount = queryChain.stream().filter(n -> n instanceof LogicalFilter).count();
        long templateInSubCount = templateChain.size() - templateSimpleCount;
        long queryInSubCount = queryChain.size() - querySimpleCount;

        if (templateSimpleCount > querySimpleCount) return false;
        if (templateInSubCount > queryInSubCount) return false;

        // Match the bottom input first
        if (!Match.match(templateBottom, queryBottom, model)) return false;

        // Try to find an assignment of template filters to query filters
        boolean[] used = new boolean[queryChain.size()];
        if (!assignFilters(templateChain, queryChain, 0, used, queryOperator, model)) {
            return false;
        }

        // Collect unmatched query simple filters (WeTune virtualExpr equivalent).
        // Store per-predicate-symbol so multiple filter chains in the same rule
        // don't interfere. Key: "virtualExpr_<predSymbol>" parallels "p0_context".
        List<RexNode> unmatchedConditions = new ArrayList<>();
        for (int i = 0; i < queryChain.size(); i++) {
            if (!used[i] && queryChain.get(i) instanceof LogicalFilter) {
                unmatchedConditions.add(((LogicalFilter) queryChain.get(i)).getCondition());
            }
        }
        if (!unmatchedConditions.isEmpty()) {
            // Find the predicate symbol from the first matched simple template filter
            String predSymbol = null;
            for (RelNode tmpl : templateChain) {
                if (tmpl instanceof LogicalFilter && !isInSubFilter(tmpl)) {
                    predSymbol = extractPredSymbol((LogicalFilter) tmpl);
                    if (predSymbol != null) break;
                }
            }
            if (predSymbol != null) {
                model.putExtra("virtualExpr_" + predSymbol,
                        new Object[]{unmatchedConditions, queryBottom});
            }
        }

        return true;
    }

    /**
     * Backtracking assignment: for each template filter, try each unmatched query filter.
     * Simple template filters only match LogicalFilter query nodes.
     * InSub template filters only match LogicalInSubFilter query nodes.
     */
    private static boolean assignFilters(List<RelNode> templateChain,
                                          List<RelNode> queryChain,
                                          int templateIdx,
                                          boolean[] used,
                                          RelNode queryOperator,
                                          Model model) {
        if (templateIdx >= templateChain.size()) return true;

        RelNode templateFilter = templateChain.get(templateIdx);
        boolean templateIsInSub = isInSubFilter(templateFilter);

        for (int qi = 0; qi < queryChain.size(); qi++) {
            if (used[qi]) continue;

            RelNode queryFilter = queryChain.get(qi);
            boolean queryIsInSub = queryFilter instanceof LogicalInSubFilter;

            // Type must match: simple↔simple, InSub↔InSub
            if (templateIsInSub != queryIsInSub) continue;

            Model derived = model.derive();
            boolean matched;
            if (templateIsInSub) {
                // Match InSub template against InSub query
                matched = matchInSubPair(
                        (LogicalFilter) templateFilter, (LogicalInSubFilter) queryFilter, derived);
            } else {
                // Match simple filter conditions
                matched = Match.matchRexNode(
                        ((LogicalFilter) templateFilter).getCondition(),
                        ((LogicalFilter) queryFilter).getCondition(),
                        queryOperator, derived);
            }

            if (matched && derived.checkConstraints()) {
                // Replay into real model
                if (templateIsInSub) {
                    matchInSubPair((LogicalFilter) templateFilter, (LogicalInSubFilter) queryFilter, model);
                } else {
                    Match.matchRexNode(
                            ((LogicalFilter) templateFilter).getCondition(),
                            ((LogicalFilter) queryFilter).getCondition(),
                            queryOperator, model);
                }
                used[qi] = true;
                if (assignFilters(templateChain, queryChain, templateIdx + 1, used, queryOperator, model)) {
                    return true;
                }
                used[qi] = false;
            }
        }

        return false;
    }

    /**
     * Match an InSub template (LogicalFilter with RexSubQuery condition) against
     * a LogicalInSubFilter query node. Binds the subquery RelNode and correlation attrs.
     */
    private static boolean matchInSubPair(LogicalFilter template, LogicalInSubFilter query, Model model) {
        RexSubQuery templateSub = extractRexSubQuery(template.getCondition());
        if (templateSub == null) return false;

        // Match subquery: template's RexSubQuery.rel against query's right child
        if (!Match.match(templateSub.rel, query.getRight(), model)) return false;

        // Bind correlation attrs from the IN operands
        RelNode queryInput = Match.unwrapHepVertex(query.getLeft());
        for (RexNode operand : templateSub.getOperands()) {
            if (operand instanceof org.apache.calcite.rex.RexInputRef) {
                int templateIdx = ((org.apache.calcite.rex.RexInputRef) operand).getIndex();
                List<String> inputFieldNames = template.getInput().getRowType().getFieldNames();
                if (templateIdx < inputFieldNames.size()) {
                    String fieldName = inputFieldNames.get(templateIdx);
                    if (org.autorewriter.rewriter.rule.symbol.SymbolKind.isSymbolName(fieldName)
                            && fieldName.charAt(0) == 'a') {
                        org.autorewriter.rewriter.rule.symbol.Symbol attrsSym =
                                org.autorewriter.rewriter.rule.symbol.Symbol.of(fieldName);
                        List<org.autorewriter.rewriter.rule.util.ColumnRef> columnRefs = new ArrayList<>();
                        RexNode queryLhsRef = query.getLhsRef();
                        if (queryLhsRef instanceof org.apache.calcite.rex.RexInputRef) {
                            int qIdx = ((org.apache.calcite.rex.RexInputRef) queryLhsRef).getIndex();
                            columnRefs.add(org.autorewriter.rewriter.rule.util.ColumnRefResolver.resolve(qIdx, queryInput));
                        }
                        if (!model.assign(attrsSym, columnRefs)) return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Check if a template LogicalFilter represents an InSubFilter
     * (its condition contains a RexSubQuery).
     */
    private static boolean isInSubFilter(RelNode node) {
        if (!(node instanceof LogicalFilter)) return false;
        return extractRexSubQuery(((LogicalFilter) node).getCondition()) != null;
    }

    private static RexSubQuery extractRexSubQuery(RexNode node) {
        if (node instanceof RexSubQuery) return (RexSubQuery) node;
        if (node instanceof org.apache.calcite.rex.RexCall) {
            for (RexNode operand : ((org.apache.calcite.rex.RexCall) node).getOperands()) {
                RexSubQuery sub = extractRexSubQuery(operand);
                if (sub != null) return sub;
            }
        }
        return null;
    }

    // ── Predicate symbol extraction ─────────────────────────────────────

    /**
     * Extract the predicate symbol name (e.g., "p0") from a template filter's condition.
     * Template conditions parsed from rule DSL are {@link RexCall} nodes whose operator
     * name is a predicate placeholder ({@code p\d+}).
     *
     * @return the predicate symbol name, or {@code null} if not a placeholder
     */
    private static String extractPredSymbol(LogicalFilter filter) {
        RexNode cond = filter.getCondition();
        if (cond instanceof RexCall) {
            String opName = ((RexCall) cond).getOperator().getName();
            if (SymbolKind.isSymbolName(opName) && opName.charAt(0) == 'p') {
                return opName;
            }
        }
        return null;
    }

    // ── Chain collection ──────────────────────────────────────────────────

    /**
     * Collect all filter-like nodes (LogicalFilter and LogicalInSubFilter)
     * in a chain from head to bottom. For LogicalInSubFilter, follows the
     * left child (main input) as the chain continuation.
     */
    static List<RelNode> collectGeneralizedFilterChain(RelNode head) {
        List<RelNode> chain = new ArrayList<>();
        RelNode current = head;
        while (true) {
            current = Match.unwrapHepVertex(current);
            if (current instanceof LogicalFilter) {
                chain.add(current);
                current = ((LogicalFilter) current).getInput();
            } else if (current instanceof LogicalInSubFilter) {
                chain.add(current);
                current = ((LogicalInSubFilter) current).getLeft();
            } else {
                break;
            }
        }
        return chain;
    }

    /**
     * Get the input below the entire generalized filter chain.
     */
    static RelNode getGeneralizedChainInput(RelNode head) {
        RelNode current = head;
        while (true) {
            current = Match.unwrapHepVertex(current);
            if (current instanceof LogicalFilter) {
                current = ((LogicalFilter) current).getInput();
            } else if (current instanceof LogicalInSubFilter) {
                current = ((LogicalInSubFilter) current).getLeft();
            } else {
                return current;
            }
        }
    }

    // ── Legacy entry points (for backward compatibility) ──────────────────

    /**
     * Collect LogicalFilter-only chain (used by existing callers).
     */
    static List<LogicalFilter> collectFilterChain(LogicalFilter head) {
        List<LogicalFilter> chain = new ArrayList<>();
        RelNode current = head;
        while (current instanceof LogicalFilter) {
            chain.add((LogicalFilter) current);
            current = Match.unwrapHepVertex(((LogicalFilter) current).getInput());
        }
        return chain;
    }

    static RelNode getChainInput(LogicalFilter head) {
        RelNode current = head;
        while (current instanceof LogicalFilter) {
            current = Match.unwrapHepVertex(((LogicalFilter) current).getInput());
        }
        return current;
    }
}
