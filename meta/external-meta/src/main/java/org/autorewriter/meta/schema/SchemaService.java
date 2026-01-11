package org.autorewriter.meta.schema;

import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.Function;
import org.apache.calcite.schema.Table;

import com.google.common.collect.Multimap;


import java.util.List;
import java.util.Map;

/**
 * schema service that get table/subSchema from meta module or hive, ch etc. remote engine.
 * */
public interface SchemaService {
    Table getTable(List<String> parents, String tableName) throws Exception;

    ProxySchema getSubSchema(List<String> parents, String subSchemaName) throws Exception;

    Multimap<String, Function> getFunctionMultimap();

    void registerFunction(String functionName, Function function);

    Map<String, RelProtoDataType> getTypeMap();

    void registerType(String typeName, RelProtoDataType type);
}
