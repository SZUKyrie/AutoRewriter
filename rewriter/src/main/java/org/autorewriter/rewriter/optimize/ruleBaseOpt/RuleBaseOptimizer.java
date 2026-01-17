package org.autorewriter.rewriter.optimize.ruleBaseOpt;

import lombok.Getter;
import lombok.Setter;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.hep.HepMatchOrder;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.autorewriter.rewriter.rule.AutoRewriteRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based optimizer using Calcite's HepPlanner.
 *
 * <p>This optimizer allows registering custom rewrite rules (AutoRewriteRule)
 * and applying them to query plans for optimization.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Register multiple rewrite rules</li>
 *   <li>Apply rules to RelNode query plans</li>
 *   <li>Support different matching strategies (top-down, bottom-up, etc.)</li>
 *   <li>Control rule application order and iterations</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
 * optimizer.addRule(myRewriteRule);
 * RelNode optimizedPlan = optimizer.optimize(originalPlan);
 * }</pre>
 */
@Setter
@Getter
public class RuleBaseOptimizer {

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

    /**
     * Add a rewrite rule to the optimizer.
     *
     * @param rule the AutoRewriteRule to add
     * @return this optimizer for chaining
     */
    public RuleBaseOptimizer addRule(AutoRewriteRule rule) {
        this.rules.add(rule);
        this.plannerNeedsRebuild = true;
        return this;
    }

    /**
     * Add a general RelOptRule to the optimizer.
     *
     * @param rule the RelOptRule to add
     * @return this optimizer for chaining
     */
    public RuleBaseOptimizer addRule(RelOptRule rule) {
        this.rules.add(rule);
        this.plannerNeedsRebuild = true;
        return this;
    }

    /**
     * Set the match order for rule application.
     *
     * <p>Available match orders:</p>
     * <ul>
     *   <li>TOP_DOWN: Match from root to leaves (default)</li>
     *   <li>BOTTOM_UP: Match from leaves to root</li>
     *   <li>ARBITRARY: Match in arbitrary order</li>
     *   <li>DEPTH_FIRST: Depth-first traversal</li>
     * </ul>
     *
     * @param matchOrder the match order to use
     * @return this optimizer for chaining
     */
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

    /**
     * Optimize a query plan by applying all registered rules.
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Ensure HepPlanner is built (lazy initialization)</li>
     *   <li>Set the root RelNode</li>
     *   <li>Find the best plan (apply all rules)</li>
     *   <li>Return the optimized RelNode</li>
     * </ol>
     *
     * @param root the original query plan (RelNode)
     * @return the optimized query plan
     */
    public RelNode optimize(RelNode root) {
        if (rules.isEmpty()) {
            return root;
        }

        // Ensure planner is built
        ensurePlannerBuilt();

        // Set root and optimize
        planner.setRoot(root);
        return planner.findBestExp();
    }

    /**
     * Clear all registered rules.
     *
     * @return this optimizer for chaining
     */
    public RuleBaseOptimizer clearRules() {
        this.rules.clear();
        this.plannerNeedsRebuild = true;
        return this;
    }

    /**
     * Get the number of registered rules.
     *
     * @return the number of rules
     */
    public int getRuleCount() {
        return this.rules.size();
    }
}
