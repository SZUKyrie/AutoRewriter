package org.autorewriter.rewriter.optimize.trace;

import lombok.Getter;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.rel.RelNode;

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
        return String.format("[Step %d] Rule: %s | %s => %s",
                stepIndex,
                rule.getClass().getSimpleName(),
                matchedRelNode.getRelTypeName(),
                producedRelNode.getRelTypeName());
    }
}

