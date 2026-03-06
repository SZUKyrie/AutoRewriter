package org.autorewriter.rewriter.optimize.costBaseOpt;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.adapter.jdbc.JdbcConvention;
import org.apache.calcite.adapter.jdbc.JdbcRules;
import org.apache.calcite.adapter.jdbc.JdbcToEnumerableConverterRule;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.plan.ConventionTraitDef;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCostFactory;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.plan.volcano.AbstractConverter;
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.DefaultRelMetadataProvider;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;
import org.autorewriter.rewriter.optimize.BaseOptimizer;
import org.autorewriter.rewriter.optimize.costBaseOpt.postgres.PostgresTableScanRule;
import org.autorewriter.rewriter.optimize.trace.OptimizationTrace;
import org.autorewriter.rewriter.optimize.trace.RuleTraceListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Cost-based optimizer using Calcite's VolcanoPlanner.
 * <p>
 * Uses a JdbcConvention with PostgreSQL dialect as the physical target convention.
 * Reuses Calcite's JDBC adapter physical operators (JdbcProject, JdbcFilter,
 * JdbcJoin, JdbcAggregate, JdbcSort) and adds a custom PostgresTableScan
 * to bridge the project's AbstractTable-based tables.
 */
@Slf4j
@Getter
@Setter
public class CostBaseOptimizer implements BaseOptimizer {

    private final List<RelOptRule> rules;
    private RelOptCostFactory costFactory;
    private final JdbcConvention convention;

    public CostBaseOptimizer() {
        this.rules = new ArrayList<>();
        this.convention = JdbcConvention.of(
                PostgresqlSqlDialect.DEFAULT,
                Expressions.constant(null),
                "POSTGRES");
    }

    public CostBaseOptimizer(RelOptCostFactory costFactory) {
        this();
        this.costFactory = costFactory;
    }

    public CostBaseOptimizer addRule(RelOptRule rule) {
        this.rules.add(rule);
        return this;
    }

    public CostBaseOptimizer clearDefaultRules() {
        this.rules.clear();
        return this;
    }

    @Override
    public RelNode optimize(RelNode root) {
        return optimize(root, null);
    }

    public RelNode optimize(RelNode root, OptimizationTrace trace) {
        VolcanoPlanner planner;
        if (costFactory != null) {
            planner = new VolcanoPlanner(costFactory, null);
        } else {
            planner = new VolcanoPlanner();
        }

        planner.addRelTraitDef(ConventionTraitDef.INSTANCE);
        planner.addRelTraitDef(RelCollationTraitDef.INSTANCE);

        // IMPORTANT: All conversion rules must be registered BEFORE changeTraits/setRoot.
        // In the custom Calcite 1.38.0-U6 fork, rules added via addRule() during
        // findBestExp() do NOT retroactively fire against already-registered nodes.
        // JdbcConvention.register() auto-registration happens too late (triggered by
        // the first JdbcConvention node), so we must register all rules eagerly here.

        // Register JDBC conversion rules (exclude JdbcToEnumerableConverterRule
        // since we target JdbcConvention, not EnumerableConvention)
        for (RelOptRule rule : JdbcRules.rules(convention)) {
            if (!(rule instanceof JdbcToEnumerableConverterRule)) {
                planner.addRule(rule);
            }
        }

        // Register table scan conversion rule (bridges AbstractTable to JdbcConvention)
        planner.addRule(PostgresTableScanRule.create(convention));

        // AbstractConverter expansion rule helps the planner bridge convention gaps
        planner.addRule(AbstractConverter.ExpandConversionRule.Config.DEFAULT.toRule());

        // Register user-added rules (logical transformations, AutoRewriteRules, etc.)
        for (RelOptRule rule : rules) {
            planner.addRule(rule);
        }

        if (trace != null) {
            planner.addListener(new RuleTraceListener(trace));
        }

        root.getCluster().setMetadataProvider(DefaultRelMetadataProvider.INSTANCE);

        // Replace the planner on the existing cluster.
        // The RelNode was created by SqlAnalyzer with a different planner (from
        // MultiDialectPlanner). We need to inject our VolcanoPlanner into the shared
        // RelOptCluster. This reflection hack is fragile on Java 16+ where
        // setAccessible may be restricted.
        RelOptCluster cluster = root.getCluster();
        try {
            java.lang.reflect.Field plannerField =
                    RelOptCluster.class.getDeclaredField("planner");
            plannerField.setAccessible(true);
            plannerField.set(cluster, planner);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set planner on RelOptCluster", e);
        }

        // Set desired physical convention on the root
        RelTraitSet desiredTraits = root.getTraitSet()
                .replace(convention);
        root = planner.changeTraits(root, desiredTraits);
        planner.setRoot(root);

        RelNode bestPlan = planner.findBestExp();
        log.info("CBO optimization completed, {} user rules + JDBC conversion rules registered",
                rules.size());

        return bestPlan;
    }

    public int getRuleCount() {
        return this.rules.size();
    }
}
