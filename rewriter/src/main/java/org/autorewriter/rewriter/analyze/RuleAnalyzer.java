package org.autorewriter.rewriter.analyze;

import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.shardingsphere.sqlfederation.autorewriter.RewriteRuleParser;
import org.apache.shardingsphere.sqlfederation.compiler.sql.ast.template.TemplateRewriteRule;
import org.autorewriter.common.enums.ComputeEngine;
import org.autorewriter.common.utils.ComputeEngineUtils;
import org.autorewriter.meta.schema.CalciteSchemaRegistry;
import org.autorewriter.rewriter.exception.RuleAnalyzeException;
import org.autorewriter.sql.analyze.AnalysisContext;
import org.autorewriter.sql.analyze.MultiDialectPlanner;
import org.autorewriter.sql.common.PlannerContext;
import org.autorewriter.sql.common.PlannerContextFactory;
import org.autorewriter.sql.common.RelDataTypeSystemRegistry;
import org.autorewriter.sql.exception.SqlAnalyzeException;

import static org.autorewriter.sql.analyze.AnalysisConfigs.*;
import static org.autorewriter.sql.analyze.AnalysisConfigs.COMMON_SQL_REX_CONVERTLET_TABLE;

public class RuleAnalyzer {
    public static RuleAnalysisContext analyze(String ruleStr) {
        TemplateRewriteRule ruleAst;
        try {
            RewriteRuleParser parser = RewriteRuleParser.createParser();
            ruleAst = parser.parse(ruleStr.trim());
        } catch (Exception e) {
            throw new RuleAnalyzeException("Rule analyze error: " + e.getMessage());
        }

        AnalysisContext srouceContext = analyze(ruleAst.getSourceTemplate(), ComputeEngine.REWRITE_RULE);
        AnalysisContext targetContext = analyze(ruleAst.getTargetTemplate(), ComputeEngine.REWRITE_RULE);

        return null;
    }

    public static AnalysisContext analyze(SqlNode queryNode, ComputeEngine computeEngine)
            throws SqlAnalyzeException {
        try {
            Planner planner = getPlanner(computeEngine);
            SqlNode validatedNode = planner.validate(queryNode);
            RelNode relNode = planner.rel(validatedNode).project();
            return new AnalysisContext(planner, relNode);
        } catch (Exception e) {
            throw new RuleAnalyzeException("Rule validate error: " + e.getMessage());
        }
    }

    private static Planner getPlanner(ComputeEngine computeEngine) {
        CalciteSchema rootSchema = CalciteSchemaRegistry.getCalciteSchema(computeEngine.getTableEngine());
        PlannerContext plannerContext = PlannerContextFactory.create(computeEngine);
        RelDataTypeSystem relDataTypeSystem = RelDataTypeSystemRegistry.getRelDataTypeSystem(computeEngine.getTableEngine());
        final FrameworkConfig config = Frameworks.newConfigBuilder()
                .parserConfig(COMMON_PARSER_CONFIG)
                .sqlValidatorConfig(createValidatorConfig(ComputeEngineUtils.getNullCollation(computeEngine)))
                .sqlToRelConverterConfig(COMMON_CONVERTER_CONFIG)
                .context(plannerContext)
                .typeSystem(relDataTypeSystem)
                .convertletTable(COMMON_SQL_REX_CONVERTLET_TABLE)
                //.operatorTable(HIVE_SQL_OPERATOR_TABLE)
                .defaultSchema(rootSchema.plus()).build();
        return new MultiDialectPlanner(config, computeEngine);
    }
}
