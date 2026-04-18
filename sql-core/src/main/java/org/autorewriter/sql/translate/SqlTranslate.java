package org.autorewriter.sql.translate;

import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.autorewriter.common.enums.ComputeEngine;
import org.autorewriter.common.utils.ComputeEngineUtils;
import org.autorewriter.sql.analyze.SqlAnalyzer;

public class SqlTranslate {

    public static String dialectTranslate(String sql, ComputeEngine fromEngine, ComputeEngine toEngine) {
        try {
            SqlNode sqlNode = SqlAnalyzer.parse(sql, fromEngine);
            SqlDialect targetDialect = ComputeEngineUtils.getDialect(toEngine);
            return sqlNode.toSqlString(targetDialect).getSql();
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to translate SQL from " + fromEngine + " to " + toEngine + ": " + e.getMessage(), e);
        }
    }
}
