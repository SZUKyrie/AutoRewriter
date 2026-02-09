package org.autorewriter.sql.analyze;

import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.autorewriter.common.enums.TableEngine;
import org.autorewriter.meta.schema.CalciteSchemaRegistry;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SchemaTestUtil {
    public static Schema createTestSchema() {
        GeneralTestSchema testSchema = new GeneralTestSchema();
        testSchema.addTable("test_table", createTableForTest());
        return testSchema;
    }

    public static void registerCalciteSchema(TableEngine tableEngine, CalciteSchema calciteSchema) throws NoSuchFieldException, IllegalAccessException {
        Field field = CalciteSchemaRegistry.class.getDeclaredField("ENGINE_SCHEMA_MAP");
        field.setAccessible(true);
        Map<TableEngine, CalciteSchema> schemaMap = (Map<TableEngine, CalciteSchema>) field.get(null);
        if (schemaMap.containsKey(tableEngine)) {
            throw new IllegalArgumentException(String.format("duplicate table engine %s for calcite schema", tableEngine.name()));
        }
        schemaMap.put(tableEngine, calciteSchema);
    }

    public static void deregisterCalciteSchema(TableEngine tableEngine) throws NoSuchFieldException, IllegalAccessException {
        Field field = CalciteSchemaRegistry.class.getDeclaredField("ENGINE_SCHEMA_MAP");
        field.setAccessible(true);
        Map<TableEngine, CalciteSchema> schemaMap = (Map<TableEngine, CalciteSchema>) field.get(null);
        if (schemaMap.containsKey(tableEngine)) {
            schemaMap.remove(tableEngine);
        }
    }

    public static Table createTableForTest() {
        Table testTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                List<String> columnNameList = new ArrayList<>();
                List<RelDataType> columnTypeList = new ArrayList<>();
                columnNameList.add("user_id");
                columnTypeList.add(typeFactory.createSqlType(SqlTypeName.BIGINT));
                columnNameList.add("name");
                columnTypeList.add(typeFactory.createTypeWithNullability(typeFactory.createSqlType(SqlTypeName.VARCHAR), true));
                columnNameList.add("pid");
                columnTypeList.add(typeFactory.createSqlType(SqlTypeName.INTEGER));
                columnNameList.add("money");
                columnTypeList.add(typeFactory.createSqlType(SqlTypeName.DECIMAL));
                columnNameList.add("disable");
                columnTypeList.add(typeFactory.createSqlType(SqlTypeName.BOOLEAN));
                columnNameList.add("status");
                columnTypeList.add(typeFactory.createSqlType(SqlTypeName.TINYINT));
                columnNameList.add("days");
                columnTypeList.add(typeFactory.createSqlType(SqlTypeName.SMALLINT));
                columnNameList.add("factor");
                columnTypeList.add(typeFactory.createSqlType(SqlTypeName.FLOAT));
                columnNameList.add("word_list");
                columnTypeList.add(typeFactory.createArrayType(typeFactory.createSqlType(SqlTypeName.VARCHAR), -1));
                columnNameList.add("p_date");
                columnTypeList.add(typeFactory.createSqlType(SqlTypeName.VARCHAR));
                return typeFactory.createStructType(columnTypeList, columnNameList);
            }
        };
        return testTable;
    }
}
