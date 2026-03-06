package org.autorewriter.rewriter.optimize.costBaseOpt.insub;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rex.RexSubQuery;

import com.google.common.collect.ImmutableList;

/**
 * Converts LogicalInSubFilter back to LogicalFilter(IN RexSubQuery).
 * This enables SubQueryRemoveRule to fire for decorrelation.
 */
public class InSubFilterToFilterRule extends RelOptRule {

    public static final InSubFilterToFilterRule INSTANCE = new InSubFilterToFilterRule();

    private InSubFilterToFilterRule() {
        super(operand(LogicalInSubFilter.class, Convention.NONE, any()),
                "InSubFilterToFilterRule");
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
        LogicalInSubFilter inSub = call.rel(0);

        // Reconstruct RexSubQuery(IN)
        RexSubQuery rexSubQuery = RexSubQuery.in(
                inSub.getRight(),
                ImmutableList.of(inSub.getLhsRef()));

        // Create LogicalFilter with the RexSubQuery as condition
        LogicalFilter filter = LogicalFilter.create(inSub.getLeft(), rexSubQuery);
        call.transformTo(filter);
    }
}
