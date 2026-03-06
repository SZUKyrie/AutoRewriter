package org.autorewriter.rewriter.optimize.costBaseOpt.insub;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttleImpl;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.rex.RexSubQuery;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.sql.SqlKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Preprocessor that converts LogicalFilter nodes containing IN RexSubQuery
 * conditions into LogicalInSubFilter nodes, exposing the subquery as a regular
 * child (visible to VolcanoPlanner for join reordering etc.).
 * <p>
 * This must run BEFORE the VolcanoPlanner, because if left as a rule inside
 * VolcanoPlanner, JdbcFilterRule creates a cheaper JdbcFilter(IN) alternative
 * (subquery cost hidden in RexNode), causing the planner to never choose the
 * InSubFilter path.
 * <p>
 * Also descends into RexSubQuery to process nested subquery RelNode trees.
 * <p>
 * Usage: call {@code InSubFilterExpander.expand(relNode)} after
 * {@code FilterSplitter.split()} and before CBO optimization.
 */
public class InSubFilterExpander extends RelShuttleImpl {

    /**
     * RexShuttle that descends into RexSubQuery and applies InSubFilterExpander
     * to the subquery's RelNode tree (for nested subqueries).
     */
    private final RexShuttle rexShuttle = new RexShuttle() {
        @Override
        public RexNode visitSubQuery(RexSubQuery subQuery) {
            RelNode newRel = subQuery.rel.accept(InSubFilterExpander.this);
            if (newRel != subQuery.rel) {
                return subQuery.clone(newRel);
            }
            return subQuery;
        }
    };

    public static RelNode expand(RelNode root) {
        return root.accept(new InSubFilterExpander());
    }

    @Override
    public RelNode visit(LogicalFilter filter) {
        // First, recursively process the direct child
        RelNode newInput = filter.getInput().accept(this);

        // Process RexSubQuery nodes inside the condition (for nested subqueries)
        RexNode condition = filter.getCondition().accept(rexShuttle);

        // Check if this filter contains an IN subquery
        RexSubQuery inSub = RexUtil.SubQueryFinder.find(condition);
        if (inSub == null || inSub.getKind() != SqlKind.IN) {
            // No IN subquery — return filter with possibly updated input/condition
            if (newInput == filter.getInput() && condition == filter.getCondition()) {
                return filter;
            }
            return filter.copy(filter.getTraitSet(), newInput, condition);
        }

        // Extract lhsRef (left-hand-side column reference of IN)
        RexNode lhsRef = inSub.getOperands().get(0);

        // Create InSubFilter: left=filter's input, right=subquery's RelNode
        // The subquery's RelNode has already been processed by the rexShuttle above,
        // so nested IN subqueries within it are also expanded.
        LogicalInSubFilter inSubFilter = LogicalInSubFilter.create(
                newInput, inSub.rel, lhsRef);

        // Handle remaining conditions (if condition is AND with other predicates)
        List<RexNode> remaining = removeSubQuery(condition, inSub);
        if (!remaining.isEmpty()) {
            RexNode remainingCondition = RexUtil.composeConjunction(
                    filter.getCluster().getRexBuilder(), remaining);
            return LogicalFilter.create(inSubFilter, remainingCondition);
        }
        return inSubFilter;
    }

    /**
     * Remove the specific RexSubQuery from a conjunction, returning remaining terms.
     */
    private static List<RexNode> removeSubQuery(RexNode condition, RexSubQuery target) {
        List<RexNode> conjunctions = RexUtil.flattenAnd(
                Collections.singletonList(condition));
        List<RexNode> remaining = new ArrayList<>();
        for (RexNode conj : conjunctions) {
            if (conj != target) {
                remaining.add(conj);
            }
        }
        return remaining;
    }
}
