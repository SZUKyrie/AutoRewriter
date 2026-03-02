package org.autorewriter.rewriter.rule.match;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.autorewriter.rewriter.rule.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles filter chain combinatorial matching.
 *
 * <p>When a template has stacked filters (e.g. {@code Filter(Filter(Input))}),
 * the query may have the same filters in a different order. This class tries
 * all permutations of assigning template filters to query filters using
 * backtracking with {@link Model#derive()}.
 */
public class FilterMatcher {

    /**
     * Match a template filter chain against a query filter chain.
     * Template chain: Filter(Filter(...(Input)))
     * Query may have a different arrangement of filters.
     */
    public static boolean matchFilterChain(LogicalFilter templateHead, LogicalFilter queryHead, Model model) {
        // Collect template filter chain
        List<LogicalFilter> templateChain = collectFilterChain(templateHead);
        // Collect query filter chain
        List<LogicalFilter> queryChain = collectFilterChain(queryHead);

        if (templateChain.size() > queryChain.size()) return false;

        // Get the bottom-most input (below all filters)
        RelNode templateBottom = getChainInput(templateHead);
        RelNode queryBottom = getChainInput(queryHead);

        // Match the bottom input first
        if (!Match.match(templateBottom, queryBottom, model)) return false;

        // Try to find an assignment of template filters to query filters
        // Use backtracking with model.derive()
        return assignFilters(templateChain, queryChain, 0, new boolean[queryChain.size()], queryHead, model);
    }

    /**
     * Backtracking assignment: for each template filter, try each unmatched query filter.
     */
    private static boolean assignFilters(List<LogicalFilter> templateChain,
                                          List<LogicalFilter> queryChain,
                                          int templateIdx,
                                          boolean[] used,
                                          RelNode queryOperator,
                                          Model model) {
        if (templateIdx >= templateChain.size()) return true; // all template filters matched

        LogicalFilter templateFilter = templateChain.get(templateIdx);

        for (int qi = 0; qi < queryChain.size(); qi++) {
            if (used[qi]) continue;

            LogicalFilter queryFilter = queryChain.get(qi);

            // Try matching in a derived model first (for backtracking)
            Model derived = model.derive();
            if (Match.matchRexNode(templateFilter.getCondition(), queryFilter.getCondition(),
                    queryOperator, derived)) {
                if (derived.checkConstraints()) {
                    // Tentatively succeed - now try matching directly in the real model
                    if (Match.matchRexNode(templateFilter.getCondition(), queryFilter.getCondition(),
                            queryOperator, model)) {
                        used[qi] = true;
                        if (assignFilters(templateChain, queryChain, templateIdx + 1,
                                used, queryOperator, model)) {
                            return true;
                        }
                        used[qi] = false;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Collect all LogicalFilter nodes in a chain from head to bottom.
     * The chain is ordered top-to-bottom.
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

    /**
     * Get the input below the entire filter chain.
     */
    static RelNode getChainInput(LogicalFilter head) {
        RelNode current = head;
        while (current instanceof LogicalFilter) {
            current = Match.unwrapHepVertex(((LogicalFilter) current).getInput());
        }
        return current;
    }
}
