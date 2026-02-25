package org.autorewriter.rewriter.optimize.trace;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptListener;
import org.apache.calcite.plan.RelOptRuleCall;

/**
 * {@link RelOptListener} implementation that records every rule fire during
 * HepPlanner optimization into an {@link OptimizationTrace}.
 */
@Slf4j
public class RuleTraceListener implements RelOptListener {

    private final OptimizationTrace trace;
    private int stepCounter = 0;

    public RuleTraceListener(OptimizationTrace trace) {
        this.trace = trace;
    }

    @Override
    public void ruleAttempted(RuleAttemptedEvent event) {
        // Only log the attempt at DEBUG level, to avoid overwhelming the logs with every attempted match.
        if (event.isBefore()) {
            RelOptRuleCall call = event.getRuleCall();
            log.debug("[ATTEMPT] Rule: {}  on: {}",
                    call.getRule(), call.rel(0).getRelTypeName());
        }
    }

    @Override
    public void ruleProductionSucceeded(RuleProductionEvent event) {
        // isBefore=false means the rule has already produced the new RelNode
        if (!event.isBefore()) {
            RelOptRuleCall call = event.getRuleCall();
            stepCounter++;

            RuleApplicationStep step = new RuleApplicationStep(
                    stepCounter,
                    call.getRule(),
                    call.rel(0), // matched subtree root (before)
                    event.getRel()      // produced subtree root (after)
            );
            trace.addStep(step);
            log.debug("[FIRED  ] {}", step);
        }
    }

    @Override
    public void relEquivalenceFound(RelEquivalenceEvent event) {}

    @Override
    public void relDiscarded(RelDiscardedEvent event) {}

    @Override
    public void relChosen(RelChosenEvent event) {}
}

