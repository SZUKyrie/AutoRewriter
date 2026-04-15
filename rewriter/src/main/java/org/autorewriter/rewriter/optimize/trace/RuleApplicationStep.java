package org.autorewriter.rewriter.optimize.trace;

import lombok.Getter;
import lombok.Setter;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;

/**
 * Records a single successful rule application during optimization.
 */
@Getter
public class RuleApplicationStep {

    /** 1-based index in the overall rule-fire sequence */
    private final int stepIndex;

    /** The rule that successfully fired */
    private final RelOptRule rule;

    /**
     * The RelNode that was matched by the rule (call.rel(0)).
     * In VolcanoPlanner this may be a RelSubset (equivalence class);
     * in HepPlanner it is a concrete RelNode.
     */
    private final RelNode matchedRelNode;

    /**
     * The RelNode produced by the rule (event.getRel() in ruleProductionSucceeded).
     * This is the new subtree root AFTER the transformation.
     */
    private final RelNode producedRelNode;

    /**
     * ID of the RelSubset (equivalence class) that was matched.
     * Set to -1 when the matched node is not a RelSubset (e.g. HepPlanner).
     *
     * <p>In VolcanoPlanner, the produced node is added to a RelSet whose subsets
     * share the same equivalence class. We use this ID to link A→B:
     * if A.producedIntoSubsetId == B.matchedSubsetId, then B can follow A in a chain.
     */
    private final int matchedSubsetId;

    /**
     * ID of the RelSubset into which the produced node was registered.
     * Set to -1 when unknown (e.g. HepPlanner or when not a RelSubset context).
     *
     * <p>Populated by RuleTraceListener when it can determine the target subset.
     */
    @Setter
    private int producedIntoSubsetId = -1;

    /**
     * Snapshot of the full plan (planner root explain()) after this rule fired.
     * Only populated when the listener has access to the planner.
     */
    @Setter
    private String fullPlanAfterStep;

    public RuleApplicationStep(int stepIndex,
                               RelOptRule rule,
                               RelNode matchedRelNode,
                               RelNode producedRelNode) {
        this(stepIndex, rule, matchedRelNode, producedRelNode, -1);
    }

    public RuleApplicationStep(int stepIndex,
                               RelOptRule rule,
                               RelNode matchedRelNode,
                               RelNode producedRelNode,
                               int matchedSubsetId) {
        this.stepIndex      = stepIndex;
        this.rule           = rule;
        this.matchedRelNode = matchedRelNode;
        this.producedRelNode = producedRelNode;
        this.matchedSubsetId = matchedSubsetId;
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

