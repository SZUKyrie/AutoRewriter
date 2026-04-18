package org.autorewriter.common.utils;

import org.apache.calcite.config.NullCollation;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;
import org.autorewriter.common.enums.ComputeEngine;

public class ComputeEngineUtils {

    public static SqlDialect getDialect(ComputeEngine computeEngine) {
        switch (computeEngine) {
            case POSTGRESQL:
                return PostgresqlSqlDialect.DEFAULT;
            case MYSQL:
                return MysqlSqlDialect.DEFAULT;
            default:
                throw new IllegalArgumentException("No dialect for computeEngine: [" + computeEngine + "] found");
        }
    }

    public static String getAsyncExecutorName(ComputeEngine computeEngine) {
        switch (computeEngine) {
            default:
                throw new IllegalArgumentException("No async executor for computeEngine: [" + computeEngine + "] found");
        }
    }

    public static NullCollation getNullCollation(ComputeEngine computeEngine) {
        switch (computeEngine) {
            case POSTGRESQL:
            case SPARK:
                return NullCollation.LOW;
            case MYSQL:
                return NullCollation.HIGH;
            case CLICKHOUSE:
                return NullCollation.LAST;
            case REWRITE_RULE:
                return NullCollation.LOW;
            default:
                throw new IllegalArgumentException("No null collation for computeEngine: [" + computeEngine + "] found");
        }
    }
}
