package org.autorewriter.rewriter.optimize.trace;

import lombok.Getter;
import org.apache.calcite.rel.RelNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the complete optimization trace for a single query.
 * <p>
 * After optimization finishes you can inspect:
 * <ul>
 *   <li>{@link #getSteps()} — ordered list of every rule that fired, with the
 *       matched (before) and produced (after) RelNode for that step.</li>
 *   <li>{@link #getIntermediateRelNodes()} — the produced RelNode of each step,
 *       i.e. every intermediate tree shape in order of appearance.</li>
 * </ul>
 */
@Getter
public class OptimizationTrace {

    /** Every rule fire event, in chronological order */
    private final List<RuleApplicationStep> steps = new ArrayList<>();

    /**
     * Append one rule-fire record.
     * Called by {@code RuleTraceListener} on every {@code ruleProductionSucceeded(isBefore=false)}.
     */
    public void addStep(RuleApplicationStep step) {
        steps.add(step);
    }

    /**
     * Returns the produced (intermediate / final) RelNode of each step in order.
     * The last element equals the final optimized tree returned by {@code findBestExp()}.
     */
    public List<RelNode> getIntermediateRelNodes() {
        List<RelNode> nodes = new ArrayList<>(steps.size());
        for (RuleApplicationStep step : steps) {
            nodes.add(step.getProducedRelNode());
        }
        return Collections.unmodifiableList(nodes);
    }

    /** Convenience: how many rules actually fired (changed the plan) */
    public int firedCount() {
        return steps.size();
    }

    /**
     * Human-readable summary, e.g. for logging.
     * <pre>
     * Optimization Trace (3 rules fired):
     *   [Step 1] Rule: AggregateReduceRule | LogicalAggregate => LogicalAggregate
     *   [Step 2] ...
     * </pre>
     */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Optimization Trace (").append(steps.size()).append(" rules fired):\n");
        for (RuleApplicationStep step : steps) {
            sb.append("  ").append(step).append("\n");
        }
        return sb.toString();
    }

    /**
     * Export the rewrite exploration tree as a PNG image via Graphviz.
     *
     * @param outputPath file path for the output PNG
     * @throws IOException if DOT generation or rendering fails
     */
    public void exportTreePng(String outputPath) throws IOException {
        TraceTreeVisualizer.exportToPng(this, outputPath);
    }
}

