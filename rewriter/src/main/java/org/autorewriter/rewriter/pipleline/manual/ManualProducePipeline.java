package org.autorewriter.rewriter.pipleline.manual;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.rules.ProjectMergeRule;
import org.apache.calcite.rel.rules.SubQueryRemoveRule;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.AnsiSqlDialect;
import org.autorewriter.rewriter.analyze.RuleAnalysisContext;
import org.autorewriter.rewriter.historical.HistoricalSqlRecord;

import java.util.List;
import org.autorewriter.rewriter.optimize.OptimizeResult;
import org.autorewriter.rewriter.optimize.ruleBaseOpt.RuleBaseOptimizer;
import org.autorewriter.rewriter.optimize.trace.OptimizationTrace;
import org.autorewriter.rewriter.pipleline.ProduceContext;
import org.autorewriter.rewriter.pipleline.ProducePipeline;
import org.autorewriter.rewriter.pipleline.ProduceStage;
import org.autorewriter.rewriter.pipleline.result.ProduceResult;
import org.autorewriter.rewriter.rule.AutoRewriteRule;
import org.autorewriter.sql.analyze.SqlAnalyzer;

@Slf4j
public class ManualProducePipeline extends ProducePipeline {

    @Override
    protected ProduceStage lastStage() {
        return ProduceStage.ONLINE;
    }

    @Override
    protected ProduceResult runTheLogic(ProduceStage lastStage, ProduceContext context) {
        ProduceResult produceResult = new ProduceResult();

        // create rbo optimizer and register rules
        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(SubQueryRemoveRule.Config.FILTER.toRule());
        //optimizer.addRule(SubQueryRemoveRule.Config.PROJECT.toRule());
        //optimizer.addRule(SubQueryRemoveRule.Config.JOIN.toRule());
        optimizer.addRule(ProjectMergeRule.Config.DEFAULT.toRule());
        long ruleRegStart = System.currentTimeMillis();
        List<RuleAnalysisContext> ruleContexts = context.getRuleAnalysisContexts();
        for (int i = 0; i < ruleContexts.size(); i++) {
            RuleAnalysisContext ruleContext = ruleContexts.get(i);
            Class<? extends RelNode> rootClass =
                    (Class<? extends RelNode>) ruleContext.getSourceRelNode().getClass();
            AutoRewriteRule rule = new AutoRewriteRule(
                    RelOptRule.operand(rootClass, RelOptRule.any()),
                    ruleContext,
                    i
            );
            optimizer.addRule(rule);
        }
        produceResult.setRuleRegistrationTimeMs(System.currentTimeMillis() - ruleRegStart);
        log.info("rule registration finished, {} rules registered in {} ms",
                context.getRuleAnalysisContexts().size(), produceResult.getRuleRegistrationTimeMs());

        for(HistoricalSqlRecord historicalSqlRecord : context.getQueryId2HistoricalSqlRecord().values()) {
            OptimizeResult optimizeResult = new OptimizeResult(historicalSqlRecord.getSql(), historicalSqlRecord.getQueryId());

            // parse sql and convert to orginal logical plan
            log.info("start to optimize query {}", historicalSqlRecord.getQueryId());
            RelNode relNode = SqlAnalyzer.analyze(historicalSqlRecord.getSql(), context.getComputeEngine()).getRelNode();
            log.info("original query logical plan:\n {}", relNode.explain());

            OptimizationTrace trace = new OptimizationTrace();
            long startTime = System.currentTimeMillis();
            RelNode optimizedRelNode = optimizer.optimize(relNode, trace);
            long endTime = System.currentTimeMillis();
            long optimizationTimeInMs = endTime - startTime;

            optimizeResult.setTrace(trace);
            if (optimizedRelNode.deepEquals(relNode)) {
                log.info("query {} cannot be optimized by any rule, optimize time: {} ms", historicalSqlRecord.getQueryId(), optimizationTimeInMs);
                optimizeResult.setRewritten(false);
                optimizeResult.setOptimizationTimeInMs(optimizationTimeInMs);
                optimizeResult.setOriginalRelNode(relNode);
            } else {
                log.info("query {} is optimized. {}", historicalSqlRecord.getQueryId(), trace.summary());
                log.info("optimized query:\n {}", relNodeToSql(optimizedRelNode));
                optimizeResult.setRewritten(true);
                optimizeResult.setOptimizationTimeInMs(optimizationTimeInMs);
                optimizeResult.setOriginalRelNode(relNode);
                optimizeResult.setOptimizedRelNode(optimizedRelNode);
            }
            produceResult.getOptimizeResults().add(optimizeResult);
            log.info("finish optimize query {}", historicalSqlRecord.getQueryId());
        }
        produceResult.setSuccess(true);
        return produceResult;
    }

    private String relNodeToSql(RelNode relNode) {
        try {
            SqlDialect dialect = AnsiSqlDialect.DEFAULT;
            RelToSqlConverter converter =
                    new RelToSqlConverter(dialect);
            RelToSqlConverter.Result result =
                    converter.visitRoot(relNode);
            SqlNode sqlNode = result.asStatement();
            return sqlNode.toSqlString(dialect).getSql();
        } catch (Exception e) {
            return "Failed to convert to SQL: " + e.getMessage();
        }
    }
}
