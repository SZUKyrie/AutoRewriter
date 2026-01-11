package org.autorewriter.sql.analyze;

import com.google.common.collect.ImmutableSet;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.config.CharLiteralStyle;
import org.apache.calcite.config.NullCollation;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.fun.SqlLibrary;
import org.apache.calcite.sql.fun.SqlLibraryOperatorTableFactory;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.util.SqlOperatorTables;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql2rel.SqlRexConvertletTable;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.calcite.sql2rel.StandardConvertletTable;

/**
 * Configs that is used to analyze, such as parser config, converter config etc.
 *
 * @author wangyanjing <wangyanjing@kuaishou.com>
 * Created on 2024-07-31
 */
public class AnalysisConfigs {
    public static final SqlParser.Config COMMON_PARSER_CONFIG = createParserConfig(Quoting.BACK_TICK);
    public static final SqlValidator.Config COMMON_VALIDATOR_CONFIG = createValidatorConfig(NullCollation.LOW);
    public static final SqlToRelConverter.Config COMMON_CONVERTER_CONFIG = createSqlToRelConverterConfig();
    public static final CalciteConnectionConfig COMMON_CALCITE_CONNECTION_CONFIG = createCalciteConnectionConfig();
    public static final SqlRexConvertletTable COMMON_SQL_REX_CONVERTLET_TABLE = StandardConvertletTable.INSTANCE;
    //public static final SqlOperatorTable HIVE_SQL_OPERATOR_TABLE = createHiveSqlOperatorTable();
    public static final CalciteConnectionConfig CLICKHOUSE_CALCITE_CONNECTION_CONFIG = createClickhouseCalciteConnectionConfig();

    public static SqlParser.Config createParserConfig(Quoting quoting) {
        return SqlParser.config()
                //lowercase or uppercase keyword doesn't matter
                .withCaseSensitive(false)
                .withIdentifierMaxLength(Integer.MAX_VALUE)
                .withQuoting(quoting)
                //how to escape a single quote? use double single quote
                .withCharLiteralStyles(ImmutableSet.of(CharLiteralStyle.STANDARD))
                //convert unquoted character to lowercase
                .withUnquotedCasing(Casing.TO_LOWER)
                .withQuotedCasing(Casing.UNCHANGED)
                .withConformance(SqlConformanceEnum.LENIENT);
    }

    public static SqlValidator.Config createValidatorConfig(NullCollation nullCollation) {
        return SqlValidator.Config.DEFAULT
                .withColumnReferenceExpansion(true)
                .withDefaultNullCollation(nullCollation);
    }

    private static SqlToRelConverter.Config createSqlToRelConverterConfig() {
        return SqlToRelConverter.config()
                .withTrimUnusedFields(true)
                .withRemoveSortInSubQuery(true)
                .withInSubQueryThreshold(Integer.MAX_VALUE)
                .withExpand(false);
    }

    private static CalciteConnectionConfig createCalciteConnectionConfig() {
        return CalciteConnectionConfig.DEFAULT
                .set(CalciteConnectionProperty.CASE_SENSITIVE, "false")
                .set(CalciteConnectionProperty.LENIENT_OPERATOR_LOOKUP, "true")
                .set(CalciteConnectionProperty.CONFORMANCE, SqlConformanceEnum.LENIENT.name());
    }

    private static CalciteConnectionConfig createClickhouseCalciteConnectionConfig() {
        return CalciteConnectionConfig.DEFAULT
                .set(CalciteConnectionProperty.CASE_SENSITIVE, "true")
                .set(CalciteConnectionProperty.QUOTED_CASING, Casing.UNCHANGED.name())
                .set(CalciteConnectionProperty.UNQUOTED_CASING, Casing.UNCHANGED.name());
    }
}
