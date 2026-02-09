package org.autorewriter.sql.analyze;

import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.type.RelDataTypeImpl;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.Planner;
import org.autorewriter.common.enums.ComputeEngine;
import org.autorewriter.common.enums.TableEngine;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.lang.reflect.Method;

public class PostgresqlSchemaTestBase {
    private static GeneralTestSchema schema;

    @BeforeClass
    public static void prepare() throws NoSuchFieldException, IllegalAccessException {
        schema = new GeneralTestSchema();
        schema.addTable("test_table", SchemaTestUtil.createTableForTest());
        schema.registerType("string", RelDataTypeImpl.proto(SqlTypeName.VARCHAR, Integer.MAX_VALUE, true));
        schema.registerType("double", RelDataTypeImpl.proto(SqlTypeName.DOUBLE, -1, true));
        schema.registerType("bigint", RelDataTypeImpl.proto(SqlTypeName.BIGINT, -1, true));
        SchemaTestUtil.registerCalciteSchema(TableEngine.POSTGRESQL,
                CalciteSchema.createRootSchema(false, false, "", schema));
    }

    protected Planner getPlannerForTest(ComputeEngine computeEngine) throws Exception {
        Method method = SqlAnalyzer.class.getDeclaredMethod("getPlanner", ComputeEngine.class);
        method.setAccessible(true);
        return (Planner) method.invoke(null, computeEngine);
    }

    @AfterClass
    public static void clean() throws NoSuchFieldException, IllegalAccessException {
        SchemaTestUtil.deregisterCalciteSchema(TableEngine.POSTGRESQL);
    }
}