package org.autorewriter.sql.analyze;

import org.apache.calcite.rel.RelNode;
import org.autorewriter.common.enums.ComputeEngine;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SqlAnalyzerTest extends PostgresqlSchemaTestBase {
    @Test
    public void testSqlAnalysis() {
        String sql = "SELECT max(user_id) FROM test_table WHERE user_id = 1";
        AnalysisContext sqlNode = SqlAnalyzer.analyze(sql, ComputeEngine.POSTGRESQL);
        RelNode relNode = sqlNode.getRelNode();
        assertEquals("LogicalAggregate(group=[{}], EXPR$0=[MAX($0)])\n" +
                "  LogicalProject(user_id=[$0])\n" +
                "    LogicalFilter(condition=[=($0, 1)])\n" +
                "      LogicalTableScan(table=[[test_table]])\n", relNode.explain());
    }
}
