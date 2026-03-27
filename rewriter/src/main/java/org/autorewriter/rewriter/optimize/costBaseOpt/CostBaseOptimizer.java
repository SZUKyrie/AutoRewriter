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
import org.apache.calcite.rel.metadata.ChainedRelMetadataProvider;
import org.apache.calcite.rel.metadata.JaninoRelMetadataProvider;
import com.google.common.collect.ImmutableList;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.RelMdColumnOriginsForProject;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.RelMdColumnOriginsForFilter;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.RelMdColumnOriginsForJoin;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.RelMdColumnOriginsInSubFilter;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.rel.rules.JoinCommuteRule;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;
import org.autorewriter.rewriter.optimize.BaseOptimizer;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.FilterToInSubFilterRule;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.InSubFilterExpander;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.InSubFilterToFilterRule;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.JdbcInSubFilterRule;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.SubQueryTreeResolver;
import org.autorewriter.rewriter.optimize.costBaseOpt.postgres.FilterMerger;
import org.autorewriter.rewriter.optimize.costBaseOpt.postgres.FilterSplitter;
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
        // Preprocess: split AND-conjoined filters into individual filter nodes
        root = FilterSplitter.split(root);

        root = InSubFilterExpander.expand(root);

        VolcanoPlanner planner;
        if (costFactory != null) {
            planner = new VolcanoPlanner(costFactory, null);
        } else {
            planner = new VolcanoPlanner();
        }

        planner.addRelTraitDef(ConventionTraitDef.INSTANCE);
        planner.addRelTraitDef(RelCollationTraitDef.INSTANCE);

        // Register JDBC conversion rules (exclude JdbcToEnumerableConverterRule
        // since we target JdbcConvention, not EnumerableConvention).
        for (RelOptRule rule : JdbcRules.rules(convention)) {
            if (rule instanceof JdbcToEnumerableConverterRule) continue;
            planner.addRule(rule);
        }

        // Register table scan conversion rule (bridges AbstractTable to JdbcConvention)
        planner.addRule(PostgresTableScanRule.create(convention));

        // InSubFilter: expose IN-subquery as two-child operator for CBO exploration
        planner.addRule(FilterToInSubFilterRule.INSTANCE);
        planner.addRule(InSubFilterToFilterRule.INSTANCE);
        planner.addRule(JdbcInSubFilterRule.create(convention));

        // AbstractConverter expansion rule helps the planner bridge convention gaps
        planner.addRule(AbstractConverter.ExpandConversionRule.Config.DEFAULT.toRule());

        // Logical transformation rules for join reordering
        // Commutativity: A ⋈ B → B ⋈ A (including LEFT↔RIGHT)
//        planner.addRule(CoreRules.JOIN_COMMUTE.config
//                .as(JoinCommuteRule.Config.class)
//                .withSwapOuter(true)
//                .toRule()
//        );
        // Associativity: (A ⋈ B) ⋈ C → A ⋈ (B ⋈ C)
        //planner.addRule(CoreRules.JOIN_ASSOCIATE);
        // Push join conditions into children
        //planner.addRule(CoreRules.JOIN_CONDITION_PUSH);
        // Push transitive predicates through join
        //planner.addRule(CoreRules.JOIN_PUSH_TRANSITIVE_PREDICATES);

        // Register user-added rules (logical transformations, AutoRewriteRules, etc.)
        for (RelOptRule rule : rules) {
            planner.addRule(rule);
        }

        if (trace != null) {
            planner.addListener(new RuleTraceListener(trace));
        }

        root.getCluster().setMetadataProvider(
                JaninoRelMetadataProvider.of(
                        ChainedRelMetadataProvider.of(ImmutableList.of(
                                RelMdColumnOriginsForProject.SOURCE,
                                RelMdColumnOriginsForFilter.SOURCE,
                                RelMdColumnOriginsForJoin.SOURCE,
                                RelMdColumnOriginsInSubFilter.SOURCE,
                                DefaultRelMetadataProvider.INSTANCE
                        ))));

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

        // Post-process: resolve RelSubset and LogicalInSubFilter references
        // inside RexSubQuery.rel trees. findBestExp() only resolves the main
        // plan tree via getInputs(); RexSubQuery.rel inside filter conditions
        // may still contain RelSubset wrappers and LogicalInSubFilter nodes
        // that RelToSqlConverter cannot handle.
        bestPlan = SubQueryTreeResolver.resolve(bestPlan);

        // Post-process: merge consecutive Filter nodes back into single AND-conjoined
        // filters. FilterSplitter splits them before optimization for rule matching;
        // FilterMerger reverses this to prevent RelToSqlConverter from generating
        // nested subqueries for each individual predicate.
        bestPlan = FilterMerger.merge(bestPlan);

        log.info("CBO optimization completed, {} user rules + JDBC conversion rules registered",
                rules.size());

        return bestPlan;
    }

    public int getRuleCount() {
        return this.rules.size();
    }
}
