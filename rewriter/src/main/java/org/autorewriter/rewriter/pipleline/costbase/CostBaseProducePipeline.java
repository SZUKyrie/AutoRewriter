package org.autorewriter.rewriter.pipleline.costbase;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.rules.JoinCommuteRule;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.AnsiSqlDialect;
import org.autorewriter.rewriter.analyze.RuleAnalysisContext;
import org.autorewriter.rewriter.historical.HistoricalSqlRecord;
import org.autorewriter.rewriter.optimize.OptimizeResult;
import org.autorewriter.rewriter.optimize.costBaseOpt.CostBaseOptimizer;
import org.autorewriter.rewriter.optimize.costBaseOpt.DistinctAggregateStripper;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.InSubFilterExpander;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.InSubFilterSqlConverter;
import org.autorewriter.rewriter.optimize.trace.OptimizationTrace;
import org.autorewriter.rewriter.pipleline.ProduceContext;
import org.autorewriter.rewriter.pipleline.ProducePipeline;
import org.autorewriter.rewriter.pipleline.ProduceStage;
import org.autorewriter.rewriter.pipleline.result.ProduceResult;
import org.autorewriter.rewriter.rule.AutoRewriteRule;
import org.autorewriter.sql.analyze.SqlAnalyzer;

import java.util.List;

@Slf4j
public class CostBaseProducePipeline extends ProducePipeline {

    @Override
    protected ProduceStage lastStage() {
        return ProduceStage.ONLINE;
    }

    @Override
    protected ProduceResult runTheLogic(ProduceStage lastStage, ProduceContext context) {
        ProduceResult produceResult = new ProduceResult();

        long ruleRegStart = System.currentTimeMillis();
        CostBaseOptimizer optimizer = new CostBaseOptimizer();
        List<RuleAnalysisContext> ruleContexts = context.getRuleAnalysisContexts();
        for (int i = 0; i < ruleContexts.size(); i++) {
            RuleAnalysisContext ruleContext = ruleContexts.get(i);

            // Preprocess the source template: convert Filter(IN subquery) to
            // LogicalInSubFilter so it matches the query plan structure after
            // InSubFilterExpander preprocessing in CostBaseOptimizer.
            RelNode sourceTemplate = InSubFilterExpander.expand(ruleContext.getSourceRelNode());
            RuleAnalysisContext expandedContext = new RuleAnalysisContext(
                    sourceTemplate, ruleContext.getTargetRelNode(),
                    ruleContext.getMatchConstraints(), ruleContext.getRewriteConstraints());

            // Register original rule
            Class<? extends RelNode> rootClass =
                    (Class<? extends RelNode>) expandedContext.getSourceRelNode().getClass();
            AutoRewriteRule rule = new AutoRewriteRule(
                    RelOptRule.operand(rootClass, RelOptRule.any()),
                    expandedContext,
                    i
            );
            optimizer.addRule(rule);

            // Register stripped version if source root is DISTINCT Aggregate.
            // This handles queries where Calcite elided LogicalAggregate because
            // the table's PK already guarantees uniqueness (RelBuilder.distinct()
            // checks areColumnsUnique and skips Aggregate creation).
            if (DistinctAggregateStripper.isDistinctAggregate(sourceTemplate)) {
                RuleAnalysisContext strippedContext =
                        DistinctAggregateStripper.stripBoth(expandedContext);
                Class<? extends RelNode> strippedRootClass =
                        (Class<? extends RelNode>) strippedContext.getSourceRelNode().getClass();
                AutoRewriteRule strippedRule = new AutoRewriteRule(
                        RelOptRule.operand(strippedRootClass, RelOptRule.any()),
                        strippedContext,
                        i,
                        "_stripped"
                );
                optimizer.addRule(strippedRule);
                log.info("Registered stripped DISTINCT rule variant for rule[{}]", i);
            }
        }

        produceResult.setRuleRegistrationTimeMs(System.currentTimeMillis() - ruleRegStart);
        log.info("CBO optimizer created with {} rules ({} default + {} auto-rewrite) in {} ms",
                optimizer.getRuleCount(), optimizer.getRuleCount() - ruleContexts.size(),
                ruleContexts.size(), produceResult.getRuleRegistrationTimeMs());

        for (HistoricalSqlRecord historicalSqlRecord : context.getQueryId2HistoricalSqlRecord().values()) {
            OptimizeResult optimizeResult = new OptimizeResult(historicalSqlRecord.getSql(), historicalSqlRecord.getQueryId());

            log.info("start to CBO optimize query {}", historicalSqlRecord.getQueryId());
            RelNode relNode = SqlAnalyzer.analyze(historicalSqlRecord.getSql(), context.getComputeEngine()).getRelNode();
            log.info("original query logical plan:\n {}", relNode.explain());

            OptimizationTrace trace = new OptimizationTrace();
            long startTime = System.currentTimeMillis();
            RelNode optimizedRelNode;
            try {
                optimizedRelNode = optimizer.optimize(relNode, trace);
            } catch (Exception e) {
                log.error("CBO optimization failed for query {}: {}", historicalSqlRecord.getQueryId(), e.getMessage(), e);
                optimizeResult.setRewritten(false);
                optimizeResult.setOptimizationTimeInMs(System.currentTimeMillis() - startTime);
                optimizeResult.setOriginalRelNode(relNode);
                produceResult.getOptimizeResults().add(optimizeResult);
                continue;
            }
            long optimizationTimeInMs = System.currentTimeMillis() - startTime;

            optimizeResult.setTrace(trace);
            if (optimizedRelNode.deepEquals(relNode)) {
                log.info("query {} cannot be optimized by CBO, optimize time: {} ms",
                        historicalSqlRecord.getQueryId(), optimizationTimeInMs);
                optimizeResult.setRewritten(false);
                optimizeResult.setOptimizationTimeInMs(optimizationTimeInMs);
                optimizeResult.setOriginalRelNode(relNode);
            } else {
                log.info("query {} is optimized by CBO.\n{}", historicalSqlRecord.getQueryId(), trace.derivationChains());
                log.info("optimized plan:\n{}", optimizedRelNode.explain());
                log.info("optimized query:\n {}", relNodeToSql(optimizedRelNode));
                optimizeResult.setRewritten(true);
                optimizeResult.setOptimizationTimeInMs(optimizationTimeInMs);
                optimizeResult.setOriginalRelNode(relNode);
                optimizeResult.setOptimizedRelNode(optimizedRelNode);
            }
            produceResult.getOptimizeResults().add(optimizeResult);
            log.info("finish CBO optimize query {}", historicalSqlRecord.getQueryId());
        }
        produceResult.setSuccess(true);
        return produceResult;
    }

    private String relNodeToSql(RelNode relNode) {
        try {
            SqlDialect dialect = AnsiSqlDialect.DEFAULT;
            InSubFilterSqlConverter converter = new InSubFilterSqlConverter(dialect);
            RelToSqlConverter.Result result = converter.visitRoot(relNode);
            SqlNode sqlNode = result.asStatement();
            return sqlNode.toSqlString(dialect).getSql();
        } catch (Exception e) {
            return "Failed to convert to SQL: " + e.getMessage();
        }
    }
}

