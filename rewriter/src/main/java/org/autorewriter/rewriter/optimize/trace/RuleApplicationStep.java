package org.autorewriter.rewriter.optimize.trace;

import lombok.Getter;
import lombok.Setter;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;

/**
 * Records a single successful rule application during HepPlanner optimization.
 */
@Getter
public class RuleApplicationStep {

    /** 1-based index in the overall rule-fire sequence */
    private final int stepIndex;

    /** The rule that successfully fired */
    private final RelOptRule rule;

    /**
     * The RelNode that was matched by the rule (call.rel(0)).
     * This is the subtree root BEFORE the transformation.
     */
    private final RelNode matchedRelNode;

    /**
     * The RelNode produced by the rule (event.getRel() in ruleProductionSucceeded).
     * This is the new subtree root AFTER the transformation.
     */
    private final RelNode producedRelNode;

    /**
     * Snapshot of the full plan (planner root explain()) after this rule fired.
     * Only populated when the listener has access to the planner (e.g. ManualProducePipeline).
     */
    @Setter
    private String fullPlanAfterStep;

    public RuleApplicationStep(int stepIndex,
                               RelOptRule rule,
                               RelNode matchedRelNode,
                               RelNode producedRelNode) {
        this.stepIndex = stepIndex;
        this.rule = rule;
        this.matchedRelNode = matchedRelNode;
        this.producedRelNode = producedRelNode;
    }

    @Override
    public String toString() {
        String ruleType = (rule instanceof ConverterRule) ? "Conversion" : "Logical";
        return String.format("[Step %d] [%s] %s | %s => %s",
                stepIndex,
                ruleType,
                rule.toString(),
                matchedRelNode.getRelTypeName(),
                producedRelNode.getRelTypeName());
    }
}

