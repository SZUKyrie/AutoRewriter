package org.autorewriter.sql.analyze;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.Function;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import java.util.HashMap;
import java.util.Map;

/**
 * Clickhouse schema for test
 */
public class GeneralTestSchema extends AbstractSchema {

    private final Map<String, Table> tables = new HashMap<>();

    private final Multimap<String, Function> functionMultimap = HashMultimap.create();

    private final Map<String, RelProtoDataType> typeMap = new HashMap<>();

    @Override
    protected Map<String, Table> getTableMap() {
        return tables;
    }

    @Override
    protected Multimap<String, Function> getFunctionMultimap() {
        return functionMultimap;
    }

    @Override
    protected Map<String, RelProtoDataType> getTypeMap() {
        return typeMap;
    }

    public void addTable(String tableName, Table table) {
        tables.put(tableName, table);
    }

    public void registerFunction(String functionName, Function function) {
        functionMultimap.put(functionName, function);
    }

    public void registerType(String typeName, RelProtoDataType type) {
        typeMap.put(typeName, type);
    }
}
