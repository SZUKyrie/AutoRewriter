package org.autorewriter.meta.schema.postgres;

import org.apache.calcite.rel.type.RelDataTypeImpl;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.type.SqlTypeName;
import org.autorewriter.meta.schema.AbstractSchemaService;

import java.util.List;

public class PostgresSchemaService extends AbstractSchemaService {

    {
        registerTypes();
    }

    @Override
    public Table getTable(List<String> parents, String tableName) throws Exception {
        //TODO: implement Postgres table fetching logic
        return null;
    }

    private void registerTypes() {
        registerType("string", RelDataTypeImpl.proto(SqlTypeName.VARCHAR, Integer.MAX_VALUE, true));
        registerType("timestamp", RelDataTypeImpl.proto(SqlTypeName.TIMESTAMP, true));
        registerType("double", RelDataTypeImpl.proto(SqlTypeName.DOUBLE, -1, true));
        registerType("bigint", RelDataTypeImpl.proto(SqlTypeName.BIGINT, -1, true));
        registerType("int", RelDataTypeImpl.proto(SqlTypeName.INTEGER, -1, true));
        registerType("float", RelDataTypeImpl.proto(SqlTypeName.FLOAT, -1, true));
    }
}
