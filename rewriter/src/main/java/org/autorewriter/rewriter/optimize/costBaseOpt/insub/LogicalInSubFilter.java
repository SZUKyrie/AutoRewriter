package org.autorewriter.rewriter.optimize.costBaseOpt.insub;

import lombok.Getter;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.BiRel;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;

import java.util.List;

/**
 * Logical operator representing an IN-subquery filter with two explicit children.
 * <p>
 * left  = main input (the table being filtered)
 * right = subquery input (the IN right-hand-side query)
 * lhsRef = column reference on the left side of IN (e.g., $0 in "id IN (subquery)")
 * <p>
 * Output rowType equals left's rowType (subquery does not contribute output columns).
 * This operator makes the subquery visible to VolcanoPlanner via getInputs(),
 * enabling join reordering and other optimizations inside the subquery.
 */
@Getter
public class LogicalInSubFilter extends BiRel {

    private final RexNode lhsRef;

    public LogicalInSubFilter(RelOptCluster cluster, RelTraitSet traitSet,
                              RelNode left, RelNode right, RexNode lhsRef) {
        super(cluster, traitSet, left, right);
        this.lhsRef = lhsRef;
    }

    public static LogicalInSubFilter create(RelNode left, RelNode right, RexNode lhsRef) {
        RelOptCluster cluster = left.getCluster();
        RelTraitSet traitSet = cluster.traitSetOf(Convention.NONE);
        return new LogicalInSubFilter(cluster, traitSet, left, right, lhsRef);
    }

    @Override
    protected RelDataType deriveRowType() {
        return left.getRowType();
    }

    @Override
    public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
        assert inputs.size() == 2;
        return new LogicalInSubFilter(getCluster(), traitSet,
                inputs.get(0), inputs.get(1), lhsRef);
    }

    @Override
    public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
        double leftRows = mq.getRowCount(left);
        double rightRows = mq.getRowCount(right);
        return planner.getCostFactory().makeCost(leftRows, leftRows * rightRows, 0);
    }

    @Override
    public RelWriter explainTerms(RelWriter pw) {
        return super.explainTerms(pw)
                .item("lhsRef", lhsRef);
    }
}
