package org.autorewriter.rewriter.optimize.trace;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptListener;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.rel.RelNode;

/**
 * {@link RelOptListener} implementation that records every rule fire during
 * optimization into an {@link OptimizationTrace}.
 *
 * <p>Works with both HepPlanner and VolcanoPlanner. For VolcanoPlanner,
 * it captures:
 * <ul>
 *   <li>{@code matchedSubsetId}: the RelSet.id of the matched RelSubset
 *       (identifies the equivalence class being matched)</li>
 *   <li>{@code producedIntoSubsetId}: the RelSet.id of the equivalence class
 *       that the produced node was registered into</li>
 * </ul>
 * When A.producedIntoSubsetId == B.matchedSubsetId, rule B can follow rule A
 * in a chain (A produced a node that feeds into the equivalence class B matches).
 */
@Slf4j
public class RuleTraceListener implements RelOptListener {

    private final OptimizationTrace trace;
    private final HepPlanner        hepPlanner;
    private final VolcanoPlanner    volcanoPlanner;
    private int stepCounter = 0;

    /** HepPlanner constructor (legacy). */
    public RuleTraceListener(OptimizationTrace trace) {
        this(trace, (HepPlanner) null);
    }

    /** HepPlanner constructor with plan snapshot support. */
    public RuleTraceListener(OptimizationTrace trace, HepPlanner planner) {
        this.trace          = trace;
        this.hepPlanner     = planner;
        this.volcanoPlanner = null;
    }

    /** VolcanoPlanner constructor — enables subset ID tracking. */
    public RuleTraceListener(OptimizationTrace trace, VolcanoPlanner planner) {
        this.trace          = trace;
        this.hepPlanner     = null;
        this.volcanoPlanner = planner;
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
            RelOptRuleCall call     = event.getRuleCall();
            RelNode        matched  = call.rel(0);
            RelNode        produced = event.getRel();
            stepCounter++;

            // --- matchedSubsetId: RelSet.id of the matched equivalence class ---
            int matchedSubsetId = -1;
            if (matched instanceof RelSubset) {
                matchedSubsetId = getRelSetId((RelSubset) matched);
            }

            // --- producedIntoSubsetId: RelSet.id where produced node was registered ---
            int producedIntoSubsetId = -1;
            if (volcanoPlanner != null) {
                try {
                    RelSubset producedSubset = volcanoPlanner.getSubset(produced);
                    if (producedSubset != null) {
                        producedIntoSubsetId = getRelSetId(producedSubset);
                    }
                } catch (Exception e) {
                    log.debug("Could not resolve subset for produced node at step {}", stepCounter, e);
                }
            } else if (matchedSubsetId >= 0) {
                producedIntoSubsetId = matchedSubsetId;
            }

            RuleApplicationStep step = new RuleApplicationStep(
                    stepCounter, call.getRule(), matched, produced, matchedSubsetId);
            step.setProducedIntoSubsetId(producedIntoSubsetId);

            // Capture full plan snapshot for HepPlanner if available
            if (hepPlanner != null) {
                try {
                    step.setFullPlanAfterStep(hepPlanner.getRoot().explain());
                } catch (Exception e) {
                    log.debug("Failed to capture full plan snapshot at step {}", stepCounter, e);
                }
            }

            trace.addStep(step);
            log.debug("[FIRED  ] {} matchedSet={} producedSet={}",
                    step, matchedSubsetId, producedIntoSubsetId);
        }
    }

    @Override public void relEquivalenceFound(RelEquivalenceEvent event) {}
    @Override public void relDiscarded(RelDiscardedEvent event) {}
    @Override public void relChosen(RelChosenEvent event) {}

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Reflectively reads the {@code RelSet.id} of the equivalence class that
     * contains the given subset. RelSet and its {@code id} field are package-private
     * in Calcite, so reflection is the only external access path.
     *
     * @return the RelSet id, or -1 if reflection fails
     */
    private static int getRelSetId(RelSubset subset) {
        try {
            java.lang.reflect.Method getSet =
                    RelSubset.class.getDeclaredMethod("getSet");
            getSet.setAccessible(true);
            Object relSet = getSet.invoke(subset);
            java.lang.reflect.Field idField =
                    relSet.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            return (int) idField.get(relSet);
        } catch (Exception e) {
            return -1;
        }
    }
}

