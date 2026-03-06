package org.autorewriter.rewriter.optimize.costBaseOpt.insub;

import org.apache.calcite.adapter.jdbc.JdbcConvention;
import org.apache.calcite.adapter.jdbc.JdbcImplementor;
import org.apache.calcite.adapter.jdbc.JdbcRel;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexNode;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * Rule and physical operator for InSubFilter in JdbcConvention.
 */
public class JdbcInSubFilterRule extends ConverterRule {

    public static JdbcInSubFilterRule create(JdbcConvention out) {
        return Config.INSTANCE
                .withConversion(LogicalInSubFilter.class, Convention.NONE, out,
                        "JdbcInSubFilterRule")
                .withRuleFactory(JdbcInSubFilterRule::new)
                .toRule(JdbcInSubFilterRule.class);
    }

    protected JdbcInSubFilterRule(Config config) {
        super(config);
    }

    @Override
    public @Nullable RelNode convert(RelNode rel) {
        LogicalInSubFilter inSub = (LogicalInSubFilter) rel;
        return new JdbcInSubFilter(
                rel.getCluster(),
                rel.getTraitSet().replace(out),
                convert(inSub.getLeft(),
                        inSub.getLeft().getTraitSet().replace(out)),
                convert(inSub.getRight(),
                        inSub.getRight().getTraitSet().replace(out)),
                inSub.getLhsRef());
    }

    /**
     * Physical InSubFilter operator in JdbcConvention.
     */
    public static class JdbcInSubFilter extends LogicalInSubFilter implements JdbcRel {

        public JdbcInSubFilter(RelOptCluster cluster, RelTraitSet traitSet,
                               RelNode left, RelNode right, RexNode lhsRef) {
            super(cluster, traitSet, left, right, lhsRef);
            assert getConvention() instanceof JdbcConvention;
        }

        @Override
        public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
            assert inputs.size() == 2;
            return new JdbcInSubFilter(getCluster(), traitSet,
                    inputs.get(0), inputs.get(1), getLhsRef());
        }

        @Override
        public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
            RelOptCost cost = super.computeSelfCost(planner, mq);
            if (cost == null) return null;
            return cost.multiplyBy(JdbcConvention.COST_MULTIPLIER);
        }

        @Override
        public JdbcImplementor.Result implement(JdbcImplementor implementor) {
            throw new UnsupportedOperationException(
                    "JdbcInSubFilter does not support SQL generation via JdbcImplementor");
        }
    }
}
