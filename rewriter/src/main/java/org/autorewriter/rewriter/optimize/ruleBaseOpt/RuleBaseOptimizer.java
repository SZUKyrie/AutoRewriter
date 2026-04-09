package org.autorewriter.rewriter.optimize.ruleBaseOpt;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.hep.HepMatchOrder;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rules.AggregateProjectMergeRule;
import org.apache.calcite.rel.rules.ProjectMergeRule;
import org.apache.calcite.rel.rules.ProjectRemoveRule;
import org.autorewriter.rewriter.optimize.BaseOptimizer;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.FilterToInSubFilterRule;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.InSubFilterExpander;
import org.autorewriter.rewriter.optimize.costBaseOpt.postgres.FilterMerger;
import org.autorewriter.rewriter.optimize.costBaseOpt.postgres.FilterSplitter;
import org.autorewriter.rewriter.optimize.costBaseOpt.postgres.RedundantProjectRemover;
import org.autorewriter.rewriter.optimize.trace.OptimizationTrace;
import org.autorewriter.rewriter.optimize.trace.RuleTraceListener;
import org.autorewriter.rewriter.rule.AutoRewriteRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based optimizer using Calcite's HepPlanner.
 */
@Slf4j
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
     * Default match order: BOTTOM_UP
     * Default max iterations: 10
     */
    public RuleBaseOptimizer() {
        this.rules = new ArrayList<>();
        this.matchOrder = HepMatchOrder.BOTTOM_UP;
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

        // FilterToInSubFilterRule: when Instantiation produces LogicalFilter(IN RexSubQuery)
        // as a rule target, this rule converts it to LogicalInSubFilter so that subsequent
        // rules can match it. Mirrors CBO where this rule is registered in VolcanoPlanner.
        programBuilder.addRuleInstance(FilterToInSubFilterRule.INSTANCE);

        // Add all user rules to the program
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
     */
    public RelNode optimize(RelNode root, OptimizationTrace trace) {
        return optimize(root, trace, false);
    }

    /**
     * Optimize {@code root} and record every rule-fire into {@code trace}.
     * When {@code captureFullPlan} is true, each trace step will include
     * a snapshot of the full plan after the rule fired.
     */
    public RelNode optimize(RelNode root, OptimizationTrace trace, boolean captureFullPlan) {
        if (rules.isEmpty()) {
            return root;
        }

        // Preprocess: aligned with CostBaseOptimizer
        root = FilterSplitter.split(root);
        root = InSubFilterExpander.expand(root);

        ensurePlannerBuilt();

        if (trace != null) {
            planner.addListener(new RuleTraceListener(trace,
                    captureFullPlan ? planner : null));
        }

        planner.setRoot(root);
        RelNode result = planner.findBestExp();

        // Post-process: remove redundant projects and merge consecutive projects
        result = RedundantProjectRemover.remove(result);
        result = simplifyProjects(result);
        result = RedundantProjectRemover.remove(result);

        if (trace != null) {
            trace.setRawOptimizedPlan(result);
        }

        result = FilterMerger.merge(result);

        log.info("RBO optimization completed, {} rules registered", rules.size());

        return result;
    }

    public RuleBaseOptimizer clearRules() {
        this.rules.clear();
        this.plannerNeedsRebuild = true;
        return this;
    }

    public int getRuleCount() {
        return this.rules.size();
    }

    /**
     * Simplify the plan by merging/removing redundant Project nodes using
     * Calcite's built-in rules:
     * - ProjectMergeRule: merge consecutive Project nodes
     * - ProjectRemoveRule: remove identity projections
     */
    private static RelNode simplifyProjects(RelNode root) {
        HepProgramBuilder builder = new HepProgramBuilder();
        builder.addMatchOrder(HepMatchOrder.BOTTOM_UP);
        builder.addRuleInstance(ProjectMergeRule.Config.DEFAULT.toRule());
        builder.addRuleInstance(ProjectRemoveRule.Config.DEFAULT.toRule());
        builder.addRuleInstance(AggregateProjectMergeRule.Config.DEFAULT.toRule());
        HepPlanner hep = new HepPlanner(builder.build());
        hep.setRoot(root);
        return hep.findBestExp();
    }
}
