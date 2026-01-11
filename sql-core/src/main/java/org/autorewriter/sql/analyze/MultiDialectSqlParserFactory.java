package org.autorewriter.sql.analyze;

import org.apache.shardingsphere.sqlfederation.autorewriter.MultiDialectSqlParser;
import org.autorewriter.common.enums.ComputeEngine;

public class MultiDialectSqlParserFactory {

    public static MultiDialectSqlParser createParser(ComputeEngine computeEngine) {
        return MultiDialectSqlParser.createParser(computeEngine.getDialectName());
    }
}
