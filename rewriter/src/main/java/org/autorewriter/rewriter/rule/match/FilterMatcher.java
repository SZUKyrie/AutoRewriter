package org.autorewriter.rewriter.rule.match;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexSubQuery;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.LogicalInSubFilter;
import org.autorewriter.rewriter.rule.constraint.Constraints;
import org.autorewriter.rewriter.rule.model.Model;
import org.autorewriter.rewriter.rule.symbol.Symbol;
import org.autorewriter.rewriter.rule.symbol.SymbolKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

        return doMatchFilterChain(templateChain, queryChain, templateBottom, queryBottom, model);
    }

    /**
     * Entry point when query head is LogicalInSubFilter (template is LogicalFilter).
     */
    public static boolean matchFilterChainFromInSub(LogicalFilter templateHead, LogicalInSubFilter queryHead, Model model) {
        List<RelNode> templateChain = collectGeneralizedFilterChain(templateHead);
        List<RelNode> queryChain = collectGeneralizedFilterChain(queryHead);

        RelNode templateBottom = getGeneralizedChainInput(templateHead);
        RelNode queryBottom = getGeneralizedChainInput(queryHead);

        return doMatchFilterChain(templateChain, queryChain, templateBottom, queryBottom, model);
    }

    private static boolean doMatchFilterChain(List<RelNode> templateChain, List<RelNode> queryChain,
                                               RelNode templateBottom, RelNode queryBottom,
                                               Model model) {
        // Count template simple filters (non-InSub) and InSub filters separately
        long templateSimpleCount = templateChain.stream().filter(n -> n instanceof LogicalFilter).count();
        long querySimpleCount = queryChain.stream().filter(n -> n instanceof LogicalFilter).count();
        long templateInSubCount = templateChain.size() - templateSimpleCount;
        long queryInSubCount = queryChain.size() - querySimpleCount;

        if (templateSimpleCount > querySimpleCount) return false;
        if (templateInSubCount > queryInSubCount) return false;

        // Match the bottom input first
        if (!Match.match(templateBottom, queryBottom, model)) return false;

        // Classify template filters into grouped (1:1) and free (N:1 absorbing)
        List<Integer> groupedIndices = new ArrayList<>();
        List<Integer> freeIndices = new ArrayList<>();
        classifyFilters(templateChain, model.constraints(), groupedIndices, freeIndices);

        boolean[] used = new boolean[queryChain.size()];

        // Phase 1: Match grouped + InSub filters with 1:1 backtracking
        if (!assignFiltersByIndices(templateChain, queryChain, groupedIndices, 0, used, model)) {
            return false;
        }

        // Phase 2: Match free filters — each gets one anchor match from remaining
        if (!assignFreeFilters(templateChain, queryChain, freeIndices, used, model)) {
            return false;
        }

        // Phase 3: Collect remaining unmatched query filters as virtualExpr
        collectUnmatchedAsVirtualExpr(templateChain, queryChain, used, queryBottom, model);

        return true;
    }

    // ── Filter classification (WeTune grouped vs free) ───────────────────

    /**
     * Classify template filters into "grouped" (need 1:1 matching) and "free" (N:1 absorbing).
     *
     * <p>A filter is <b>grouped</b> if:
     * <ul>
     *   <li>It is an InSub filter (always requires exact 1:1 match), OR</li>
     *   <li>Its attrs symbol shares an equivalence class with another template filter's
     *       attrs symbol (e.g., AttrsEq(a0, a1) where both a0 and a1 belong to chain filters), OR</li>
     *   <li>No attrs symbol could be extracted (conservative fallback)</li>
     * </ul>
     *
     * <p>A filter is <b>free</b> if its attrs symbol has no buddies among the chain's
     * template filters. Free filters can absorb extra query filters as virtualExprs.
     */
    private static void classifyFilters(List<RelNode> templateChain, Constraints constraints,
                                         List<Integer> groupedIndices, List<Integer> freeIndices) {
        // Extract attrs symbols for all simple template filters
        Symbol[] attrsSymbols = new Symbol[templateChain.size()];
        for (int i = 0; i < templateChain.size(); i++) {
            RelNode node = templateChain.get(i);
            if (isInSubFilter(node)) {
                groupedIndices.add(i);
            } else if (node instanceof LogicalFilter) {
                attrsSymbols[i] = extractAttrsSymbol((LogicalFilter) node);
            }
        }

        // For each non-InSub filter, check for buddies in the eq class
        for (int i = 0; i < templateChain.size(); i++) {
            if (!(templateChain.get(i) instanceof LogicalFilter) || isInSubFilter(templateChain.get(i))) {
                continue; // already handled
            }

            Symbol attrsSym = attrsSymbols[i];
            if (attrsSym == null) {
                groupedIndices.add(i); // conservative: no attrs → grouped
                continue;
            }

            boolean hasBuddy = false;
            if (constraints != null) {
                Set<Symbol> eqClass = constraints.eqClassOf(attrsSym);
                for (int j = 0; j < templateChain.size(); j++) {
                    if (i == j) continue;
                    if (attrsSymbols[j] != null && eqClass.contains(attrsSymbols[j])) {
                        hasBuddy = true;
                        break;
                    }
                }
            }

            if (hasBuddy) {
                groupedIndices.add(i);
            } else {
                freeIndices.add(i);
            }
        }
    }

    /**
     * Extract the attrs symbol (e.g., Symbol("a0")) from a template filter's predicate.
     *
     * <p>Template filter conditions are {@link RexCall} nodes with predicate placeholder
     * operators ({@code p\d+}). The operands are {@link RexInputRef} pointing to field
     * names in the filter's input row type. If a field name is an attrs symbol ({@code a\d+}),
     * it is returned.
     *
     * @return the attrs Symbol, or {@code null} if not found
     */
    private static Symbol extractAttrsSymbol(LogicalFilter filter) {
        RexNode cond = filter.getCondition();
        if (!(cond instanceof RexCall)) return null;
        RexCall call = (RexCall) cond;
        String opName = call.getOperator().getName();
        if (!SymbolKind.isSymbolName(opName) || opName.charAt(0) != 'p') return null;

        RelNode input = Match.unwrapHepVertex(filter.getInput());
        List<String> fieldNames = input.getRowType().getFieldNames();
        for (RexNode operand : call.getOperands()) {
            if (operand instanceof RexInputRef) {
                int idx = ((RexInputRef) operand).getIndex();
                if (idx < fieldNames.size()) {
                    String fieldName = fieldNames.get(idx);
                    if (SymbolKind.isSymbolName(fieldName) && fieldName.charAt(0) == 'a') {
                        return Symbol.of(fieldName);
                    }
                }
            }
        }
        return null;
    }

    // ── Phase 1: Grouped 1:1 backtracking ────────────────────────────────

    /**
     * Backtracking 1:1 assignment for grouped + InSub template filters.
     *
     * <p>Only operates on the specified template indices. Uses model.derive()
     * for backtracking. Passes individual {@code queryFilter} as context to
     * {@link Match#matchRexNode} for correct predicate context storage
     * (Step 1 fix: not the chain head).
     */
    private static boolean assignFiltersByIndices(List<RelNode> templateChain,
                                                   List<RelNode> queryChain,
                                                   List<Integer> templateIndices,
                                                   int pos,
                                                   boolean[] used,
                                                   Model model) {
        if (pos >= templateIndices.size()) return true;

        int templateIdx = templateIndices.get(pos);
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
                matched = matchInSubPair(
                        (LogicalFilter) templateFilter, (LogicalInSubFilter) queryFilter, derived);
            } else {
                // Pass templateFilter and queryFilter for correct attrs placeholder extraction
                matched = Match.matchRexNode(
                        ((LogicalFilter) templateFilter).getCondition(),
                        ((LogicalFilter) queryFilter).getCondition(),
                        templateFilter, queryFilter, derived);
            }

            boolean constraintOk = matched && derived.checkConstraints();

            if (constraintOk) {
                // Replay into real model
                if (templateIsInSub) {
                    matchInSubPair((LogicalFilter) templateFilter, (LogicalInSubFilter) queryFilter, model);
                } else {
                    Match.matchRexNode(
                            ((LogicalFilter) templateFilter).getCondition(),
                            ((LogicalFilter) queryFilter).getCondition(),
                            templateFilter, queryFilter, model);
                }
                used[qi] = true;
                if (assignFiltersByIndices(templateChain, queryChain, templateIndices, pos + 1, used, model)) {
                    return true;
                }
                used[qi] = false;
            }
        }

        return false;
    }

    // ── Phase 2: Free filter anchor matching ─────────────────────────────

    /**
     * Anchor matching for free template filters.
     *
     * <p>Each free template filter gets one anchor match from the remaining unused
     * query filters. No backtracking between free filters — each independently
     * takes the first compatible match. Remaining unmatched query filters will
     * be collected as virtualExprs in Phase 3.
     */
    private static boolean assignFreeFilters(List<RelNode> templateChain,
                                              List<RelNode> queryChain,
                                              List<Integer> freeIndices,
                                              boolean[] used,
                                              Model model) {
        for (int templateIdx : freeIndices) {
            RelNode templateFilter = templateChain.get(templateIdx);
            if (!(templateFilter instanceof LogicalFilter)) continue;

            boolean foundMatch = false;
            for (int qi = 0; qi < queryChain.size(); qi++) {
                if (used[qi]) continue;

                RelNode queryFilter = queryChain.get(qi);
                if (!(queryFilter instanceof LogicalFilter)) continue;
                if (queryFilter instanceof LogicalInSubFilter) continue;

                Model derived = model.derive();
                boolean matched = Match.matchRexNode(
                        ((LogicalFilter) templateFilter).getCondition(),
                        ((LogicalFilter) queryFilter).getCondition(),
                        templateFilter, queryFilter, derived);

                if (matched && derived.checkConstraints()) {
                    // Replay into real model
                    Match.matchRexNode(
                            ((LogicalFilter) templateFilter).getCondition(),
                            ((LogicalFilter) queryFilter).getCondition(),
                            templateFilter, queryFilter, model);
                    used[qi] = true;
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) return false;
        }
        return true;
    }

    // ── Phase 3: Collect unmatched as virtualExpr ────────────────────────

    /**
     * Collect remaining unmatched query filter conditions as virtualExpr.
     *
     * <p>Unmatched query simple filters (LogicalFilter, not InSub) are stored under
     * {@code "virtualExpr_<predSymbol>"} in the Model, keyed by the first matched
     * template filter's predicate symbol. This parallels WeTune's virtualExpr mechanism
     * and allows {@link org.autorewriter.rewriter.rule.instantiation.Instantiation#applyVirtualExprs}
     * to re-apply them during target construction.
     */
    private static void collectUnmatchedAsVirtualExpr(List<RelNode> templateChain,
                                                       List<RelNode> queryChain,
                                                       boolean[] used,
                                                       RelNode queryBottom,
                                                       Model model) {
        List<RexNode> unmatchedConditions = new ArrayList<>();
        for (int i = 0; i < queryChain.size(); i++) {
            if (!used[i] && queryChain.get(i) instanceof LogicalFilter) {
                unmatchedConditions.add(((LogicalFilter) queryChain.get(i)).getCondition());
            }
        }
        if (!unmatchedConditions.isEmpty()) {
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
     *
     * <p>Handles ShardingSphere parser merging nested {@code Filter<p1>(Filter<p0>(...))}
     * into a single {@code Filter(AND(p1, p0))}. When a LogicalFilter has a compound
     * AND condition where each operand is a predicate placeholder ({@code p\d+}), the
     * filter is split into multiple virtual LogicalFilter nodes in the chain.
     */
    static List<RelNode> collectGeneralizedFilterChain(RelNode head) {
        List<RelNode> chain = new ArrayList<>();
        RelNode current = head;
        while (true) {
            current = Match.unwrapHepVertex(current);
            if (current instanceof LogicalFilter) {
                LogicalFilter filter = (LogicalFilter) current;
                // Check for parser-merged AND(p1, p0) compound predicate placeholders
                List<RexNode> predParts = splitAndPredicatePlaceholders(filter.getCondition());
                if (predParts != null && predParts.size() > 1) {
                    // Split into virtual filters, each with a single predicate placeholder
                    for (RexNode part : predParts) {
                        chain.add(LogicalFilter.create(filter.getInput(), part));
                    }
                } else {
                    chain.add(current);
                }
                current = filter.getInput();
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
     * If a filter condition is {@code AND(p1(...), p2(...), ...)} where each
     * operand is a predicate placeholder, return the individual placeholder RexNodes.
     * This handles ShardingSphere parser merging nested {@code Filter<p3>(Filter<p2>(...))}
     * into a single {@code Filter(AND(p3, p2))}.
     *
     * @return list of individual predicate placeholder RexNodes, or null if not applicable
     */
    private static List<RexNode> splitAndPredicatePlaceholders(RexNode condition) {
        if (!(condition instanceof RexCall)) return null;
        RexCall call = (RexCall) condition;
        if (!"AND".equals(call.getOperator().getName())) return null;

        List<RexNode> parts = new ArrayList<>();
        for (RexNode operand : call.getOperands()) {
            if (!(operand instanceof RexCall)) return null;
            String opName = ((RexCall) operand).getOperator().getName();
            if (!SymbolKind.isSymbolName(opName) || opName.charAt(0) != 'p') return null;
            parts.add(operand);
        }
        return parts.isEmpty() ? null : parts;
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
