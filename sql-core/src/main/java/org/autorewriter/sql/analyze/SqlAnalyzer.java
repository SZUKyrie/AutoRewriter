package org.autorewriter.sql.analyze;

import com.google.common.base.Preconditions;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.autorewriter.common.enums.ComputeEngine;
import org.autorewriter.common.utils.ComputeEngineUtils;
import org.autorewriter.meta.schema.CalciteSchemaRegistry;
import org.autorewriter.sql.common.PlannerContext;
import org.autorewriter.sql.common.PlannerContextFactory;
import org.autorewriter.sql.common.RelDataTypeSystemRegistry;
import org.autorewriter.sql.exception.SqlAnalyzeException;
import org.autorewriter.sql.exception.SqlParseException;

import static org.autorewriter.sql.analyze.AnalysisConfigs.*;

public class SqlAnalyzer {
    public static AnalysisContext analyze(String query, ComputeEngine computeEngine)
            throws SqlParseException, SqlAnalyzeException {
        Planner planner = getPlanner(computeEngine);
        SqlNode parsedNode;
        try {
            parsedNode = planner.parse(query);
        } catch (Exception e) {
            throw new SqlParseException(e);
        }

        try {
            SqlNode validateNode = planner.validate(parsedNode);
            RelNode relNode = planner.rel(validateNode).project();
            return new AnalysisContext(planner, relNode);
        } catch (Exception e) {
            throw new SqlAnalyzeException("SQL validate error: " + e.getMessage());
        }
    }

    public static AnalysisContext analyze(SqlNode queryNode, ComputeEngine computeEngine)
            throws SqlAnalyzeException {
        try {
            Planner planner = getPlanner(computeEngine);
            SqlNode validatedNode = planner.validate(queryNode);
            RelNode relNode = planner.rel(validatedNode).project();
            return new AnalysisContext(planner, relNode);
        } catch (Exception e) {
            throw new SqlAnalyzeException("SQL validate error: " + e.getMessage());
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
