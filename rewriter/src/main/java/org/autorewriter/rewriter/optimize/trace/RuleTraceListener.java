package org.autorewriter.rewriter.optimize.trace;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptListener;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.hep.HepPlanner;

/**
 * {@link RelOptListener} implementation that records every rule fire during
 * HepPlanner optimization into an {@link OptimizationTrace}.
 */
@Slf4j
public class RuleTraceListener implements RelOptListener {

    private final OptimizationTrace trace;
    private final HepPlanner planner;
    private int stepCounter = 0;

    public RuleTraceListener(OptimizationTrace trace) {
        this(trace, null);
    }

    public RuleTraceListener(OptimizationTrace trace, HepPlanner planner) {
        this.trace = trace;
        this.planner = planner;
    }

    @Override
    public void ruleAttempted(RuleAttemptedEvent event) {
        if (event.isBefore()) {
            RelOptRuleCall call = event.getRuleCall();
            log.debug("[ATTEMPT] Rule: {}  on: {}",
                    call.getRule(), call.rel(0).getRelTypeName());
        }
    }

    @Override
    public void ruleProductionSucceeded(RuleProductionEvent event) {
        if (!event.isBefore()) {
            RelOptRuleCall call = event.getRuleCall();
            stepCounter++;

            RuleApplicationStep step = new RuleApplicationStep(
                    stepCounter,
                    call.getRule(),
                    call.rel(0), // matched subtree root (before)
                    event.getRel()      // produced subtree root (after)
            );

            // Capture full plan snapshot if planner is available
            if (planner != null) {
                try {
                    step.setFullPlanAfterStep(planner.getRoot().explain());
                } catch (Exception e) {
                    log.debug("Failed to capture full plan snapshot at step {}", stepCounter, e);
                }
            }

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

