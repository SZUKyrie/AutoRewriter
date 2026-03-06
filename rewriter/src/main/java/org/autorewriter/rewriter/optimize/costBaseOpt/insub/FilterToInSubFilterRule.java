package org.autorewriter.rewriter.optimize.costBaseOpt.insub;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexSubQuery;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.sql.SqlKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts LogicalFilter(condition containing IN RexSubQuery) to LogicalInSubFilter.
 * Only handles the case where the entire condition IS the IN subquery
 * (or the first IN subquery found). Remaining conditions stay as a Filter on top.
 */
public class FilterToInSubFilterRule extends RelOptRule {

    public static final FilterToInSubFilterRule INSTANCE = new FilterToInSubFilterRule();

    private FilterToInSubFilterRule() {
        super(operandJ(LogicalFilter.class,
                Convention.NONE, RexUtil.SubQueryFinder.FILTER_PREDICATE, any()),
                "FilterToInSubFilterRule");
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
        LogicalFilter filter = call.rel(0);
        RexNode condition = filter.getCondition();

        // Find the first IN subquery in the condition
        RexSubQuery inSub = RexUtil.SubQueryFinder.find(condition);
        if (inSub == null || inSub.getKind() != SqlKind.IN) {
            return;
        }

        // Extract lhsRef (the left-hand-side of IN)
        RexNode lhsRef = inSub.getOperands().get(0);

        // Create InSubFilter: left=filter's input, right=subquery's RelNode
        LogicalInSubFilter inSubFilter = LogicalInSubFilter.create(
                filter.getInput(), inSub.rel, lhsRef);

        // Handle remaining conditions (if condition is AND with other predicates)
        List<RexNode> remaining = removeSubQuery(condition, inSub);
        if (!remaining.isEmpty()) {
            RexNode remainingCondition = RexUtil.composeConjunction(
                    filter.getCluster().getRexBuilder(), remaining);
            call.transformTo(LogicalFilter.create(inSubFilter, remainingCondition));
        } else {
            call.transformTo(inSubFilter);
        }
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
