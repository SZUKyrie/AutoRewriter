package org.autorewriter.rewriter.optimize.costBaseOpt.insub;

import com.google.common.collect.ImmutableList;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.rex.RexSubQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-processor that resolves VolcanoPlanner-internal references in
 * {@link RexSubQuery#rel} trees within the final optimized plan.
 * <p>
 * After {@code VolcanoPlanner.findBestExp()}, the main plan tree has concrete
 * nodes, but {@code RexSubQuery.rel} inside filter conditions may still
 * reference nodes whose inputs are {@link RelSubset} wrappers (because
 * {@code findBestExp()} only follows {@code getInputs()}, not RexNode
 * expressions).
 * <p>
 * This utility:
 * <ol>
 *   <li>Replaces {@code RelSubset} with the best concrete implementation</li>
 *   <li>Resolves nested {@code RexSubQuery.rel} trees recursively</li>
 * </ol>
 * <p>
 * Note: {@link LogicalInSubFilter} nodes are preserved — they are handled
 * natively by {@link InSubFilterSqlConverter} during SQL generation.
 * <p>
 * Usage: call {@code SubQueryTreeResolver.resolve(bestPlan)} after
 * {@code VolcanoPlanner.findBestExp()}.
 */
public class SubQueryTreeResolver {

    /**
     * Resolve the final plan: fix RexSubQuery.rel trees in filter conditions.
     */
    public static RelNode resolve(RelNode plan) {
        return processNode(plan);
    }

    private static RelNode processNode(RelNode node) {
        // Process inputs first (depth-first)
        List<RelNode> newInputs = new ArrayList<>();
        boolean inputsChanged = false;
        for (RelNode input : node.getInputs()) {
            RelNode processed = processNode(input);
            newInputs.add(processed);
            if (processed != input) inputsChanged = true;
        }
        if (inputsChanged) {
            node = node.copy(node.getTraitSet(), newInputs);
        }

        // Resolve RexSubQuery in filter conditions
        if (node instanceof Filter) {
            Filter filter = (Filter) node;
            RexNode newCondition = resolveRexSubQueries(filter.getCondition());
            if (newCondition != filter.getCondition()) {
                node = filter.copy(filter.getTraitSet(), filter.getInput(), newCondition);
            }
        }

        return node;
    }

    /**
     * Apply a RexShuttle that resolves RexSubQuery.rel trees.
     */
    private static RexNode resolveRexSubQueries(RexNode rex) {
        return rex.accept(new RexShuttle() {
            @Override
            public RexNode visitSubQuery(RexSubQuery subQuery) {
                RelNode resolved = resolveRelTree(subQuery.rel);
                if (resolved != subQuery.rel) {
                    return subQuery.clone(resolved);
                }
                return subQuery;
            }
        });
    }

    /**
     * Recursively resolve a RelNode tree inside RexSubQuery:
     * <ul>
     *   <li>Replace {@code RelSubset} with best concrete node</li>
     *   <li>Replace {@code LogicalInSubFilter} with {@code LogicalFilter(IN)}
     *       (needed because FilterMatcher expects LogicalFilter in subquery trees)</li>
     *   <li>Resolve any nested {@code RexSubQuery} in filter conditions</li>
     * </ul>
     */
    private static RelNode resolveRelTree(RelNode node) {
        // Strip RelSubset → concrete node
        if (node instanceof RelSubset) {
            RelSubset subset = (RelSubset) node;
            RelNode best = subset.getBest();
            if (best == null) {
                // Fall back to any concrete node in the set
                for (RelNode rel : subset.getRels()) {
                    if (!(rel instanceof RelSubset)) {
                        best = rel;
                        break;
                    }
                }
            }
            if (best == null) {
                throw new RuntimeException("Cannot resolve empty RelSubset: " + subset);
            }
            return resolveRelTree(best);
        }

        // Recursively resolve inputs
        List<RelNode> newInputs = new ArrayList<>();
        boolean inputsChanged = false;
        for (RelNode input : node.getInputs()) {
            RelNode resolved = resolveRelTree(input);
            newInputs.add(resolved);
            if (resolved != input) inputsChanged = true;
        }
        if (inputsChanged) {
            node = node.copy(node.getTraitSet(), newInputs);
        }

        // Convert LogicalInSubFilter → LogicalFilter(IN RexSubQuery) inside subquery trees.
        // The main plan tree keeps LogicalInSubFilter (handled by InSubFilterSqlConverter),
        // but subquery trees inside RexSubQuery.rel must use LogicalFilter because
        // FilterMatcher expects it during rule matching.
        if (node instanceof LogicalInSubFilter) {
            LogicalInSubFilter inSub = (LogicalInSubFilter) node;
            RexSubQuery rexSub = RexSubQuery.in(
                    inSub.getRight(),
                    ImmutableList.of(inSub.getLhsRef()));
            node = LogicalFilter.create(inSub.getLeft(), rexSub);
        }

        // Resolve RexSubQuery in filter conditions (handles nested subqueries)
        if (node instanceof Filter) {
            Filter filter = (Filter) node;
            RexNode newCondition = resolveRexSubQueries(filter.getCondition());
            if (newCondition != filter.getCondition()) {
                node = filter.copy(filter.getTraitSet(), filter.getInput(), newCondition);
            }
        }

        return node;
    }
}
