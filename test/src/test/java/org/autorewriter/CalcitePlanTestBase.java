package org.autorewriter;

import org.autorewriter.common.enums.ComputeEngine;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

public class CalcitePlanTestBase {
    protected static final String TEST_RESOURCES_DIR = "calcite-plan-test";
    protected static final String TEST_DATABASE = "test";
    protected static final SqlPlanFixture SQL_PLAN_FIXTURE = new SqlPlanFixture();

    @Test
    public void createPGDeptTable() throws SQLException {
        SQL_PLAN_FIXTURE.withComputeEngine(ComputeEngine.POSTGRESQL).withDb(TEST_DATABASE)
                .withCreateTable(
                        "create table if not exists dept (\n" +
                                "id int, \n" + "name varchar(128), \n" + "pid int, \n" +
                                "money decimal(32, 2), \n" + "state char(5))")
                .createNewTable();
    }
}
