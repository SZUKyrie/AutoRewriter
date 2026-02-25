package org.autorewriter.sql.analyze;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.shardingsphere.sqlfederation.autorewriter.MultiDialectSqlParser;
import org.autorewriter.common.enums.ComputeEngine;
import org.autorewriter.common.utils.ComputeEngineUtils;
import org.autorewriter.meta.schema.CalciteSchemaRegistry;
import org.autorewriter.sql.common.PlannerContext;
import org.autorewriter.sql.common.PlannerContextFactory;
import org.autorewriter.sql.common.RelDataTypeSystemRegistry;
import org.autorewriter.sql.exception.SqlAnalyzeException;
import org.autorewriter.sql.exception.SqlParseException;

import static org.autorewriter.sql.analyze.AnalysisConfigs.*;

@Slf4j
public class SqlAnalyzer {
    public static AnalysisContext analyze(String query, ComputeEngine computeEngine)
            throws SqlParseException, SqlAnalyzeException {
        SqlNode parsedNode = parse(query, computeEngine);
        return analyze(parsedNode, computeEngine);
    }

    public static AnalysisContext analyze(SqlNode queryNode, ComputeEngine computeEngine)
            throws SqlAnalyzeException {
        try {
            Planner planner = getPlanner(computeEngine);
            Preconditions.checkArgument(planner instanceof MultiDialectPlanner, "Planner can not analyze SqlNode except MultiDialectPlanner");
            ((MultiDialectPlanner) planner).skipParse();
            SqlNode validatedNode = planner.validate(queryNode);
            RelNode relNode = planner.rel(validatedNode).project();
            return new AnalysisContext(planner, relNode);
        } catch (Exception e) {
            log.error("failed to analyze sql, exception: {}", ExceptionUtils.getStackTrace(e));
            throw new SqlAnalyzeException(ExceptionUtils.getStackTrace(e));
        }
    }

    public static SqlNode parse(String sql, ComputeEngine computeEngine) throws SqlParseException {
        try {
            MultiDialectSqlParser parser = MultiDialectSqlParserFactory.createParser(computeEngine);
            return parser.parse(sql);
        } catch (Exception e) {
            log.error("failed to parse sql, exception: {}", ExceptionUtils.getStackTrace(e));
            throw new SqlParseException(e);
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
