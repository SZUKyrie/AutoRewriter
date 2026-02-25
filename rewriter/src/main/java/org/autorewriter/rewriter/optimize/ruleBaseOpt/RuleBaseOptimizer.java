package org.autorewriter.rewriter.optimize.ruleBaseOpt;

import lombok.Getter;
import lombok.Setter;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.hep.HepMatchOrder;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.autorewriter.rewriter.optimize.BaseOptimizer;
import org.autorewriter.rewriter.optimize.trace.OptimizationTrace;
import org.autorewriter.rewriter.optimize.trace.RuleTraceListener;
import org.autorewriter.rewriter.rule.AutoRewriteRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based optimizer using Calcite's HepPlanner.
 */
@Setter
@Getter
public class RuleBaseOptimizer implements BaseOptimizer {

    private final List<RelOptRule> rules;
    private HepMatchOrder matchOrder;
    private int maxIterations;
    private HepPlanner planner;
    private boolean plannerNeedsRebuild;

    /**
     * Create a new RuleBaseOptimizer with default settings.
     * Default match order: TOP_DOWN
     * Default max iterations: 10
     */
    public RuleBaseOptimizer() {
        this.rules = new ArrayList<>();
        this.matchOrder = HepMatchOrder.TOP_DOWN;
        this.maxIterations = 10;
        this.plannerNeedsRebuild = true;
    }

    public RuleBaseOptimizer(HepMatchOrder matchOrder, int maxIterations) {
        this.rules = new ArrayList<>();
        this.matchOrder = matchOrder;
        this.maxIterations = maxIterations;
        this.plannerNeedsRebuild = true;
    }

    public RuleBaseOptimizer addRule(AutoRewriteRule rule) {
        this.rules.add(rule);
        this.plannerNeedsRebuild = true;
        return this;
    }

    public RuleBaseOptimizer addRule(RelOptRule rule) {
        this.rules.add(rule);
        this.plannerNeedsRebuild = true;
        return this;
    }

    public RuleBaseOptimizer setMatchOrder(HepMatchOrder matchOrder) {
        this.matchOrder = matchOrder;
        this.plannerNeedsRebuild = true;
        return this;
    }

    /**
     * Set the maximum number of iterations for rule application.
     *
     * @param maxIterations maximum iterations (must be > 0)
     * @return this optimizer for chaining
     */
    public RuleBaseOptimizer setMaxIterations(int maxIterations) {
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("maxIterations must be > 0, got: " + maxIterations);
        }
        this.maxIterations = maxIterations;
        this.plannerNeedsRebuild = true;
        return this;
    }

    /**
     * Build or rebuild the HepPlanner if needed.
     * This is called automatically when rules or configuration change.
     */
    private void ensurePlannerBuilt() {
        if (!plannerNeedsRebuild && planner != null) {
            return;
        }

        // Build HepProgram
        HepProgramBuilder programBuilder = new HepProgramBuilder();
        programBuilder.addMatchOrder(matchOrder);

        // Add all rules to the program
        for (RelOptRule rule : rules) {
            programBuilder.addRuleInstance(rule);
        }

        HepProgram program = programBuilder.build();

        // Create HepPlanner
        this.planner = new HepPlanner(program);
        this.plannerNeedsRebuild = false;
    }

    @Override
    public RelNode optimize(RelNode root) {
        return optimize(root, null);
    }

    /**
     * Optimize {@code root} and record every rule-fire into {@code trace}.
     *
     * @param root  the RelNode to optimize
     * @param trace receives one {@link org.autorewriter.rewriter.optimize.trace.RuleApplicationStep}
     *              per rule that actually fires; pass {@code null} to skip tracing
     * @return the optimized RelNode
     */
    public RelNode optimize(RelNode root, OptimizationTrace trace) {
        if (rules.isEmpty()) {
            return root;
        }

        ensurePlannerBuilt();

        if (trace != null) {
            planner.addListener(new RuleTraceListener(trace));
        }

        planner.setRoot(root);
        return planner.findBestExp();
    }

    public RuleBaseOptimizer clearRules() {
        this.rules.clear();
        this.plannerNeedsRebuild = true;
        return this;
    }

    public int getRuleCount() {
        return this.rules.size();
    }
}
