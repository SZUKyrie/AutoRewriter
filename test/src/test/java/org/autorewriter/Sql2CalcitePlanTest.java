package org.autorewriter;

import org.apache.calcite.sql.SqlDialect;
import org.autorewriter.common.enums.ComputeEngine;
import org.junit.Test;

public class Sql2CalcitePlanTest extends CalcitePlanTestBase{
    @Test
    public void testPostgresqlPlanCorrectness() throws Exception {
        SQL_PLAN_FIXTURE.withComputeEngine(ComputeEngine.POSTGRESQL).withDb(TEST_DATABASE)
                .withSql("select id, name from test.dept")
                .plan("LogicalProject(id=[$0], name=[$1])\n" +
                        "  LogicalTableScan(table=[[test, dept]])\n")
                .planSql(SqlDialect.DatabaseProduct.POSTGRESQL.getDialect(), "SELECT \"id\", \"name\"\n" +
                        "FROM \"test\".\"dept\"");
    }
}
