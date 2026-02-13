package org.autorewriter.meta.schema.postgres;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.type.RelDataTypeImpl;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.type.SqlTypeName;
import org.autorewriter.common.entity.Column;
import org.autorewriter.meta.schema.AbstractSchemaService;
import org.autorewriter.meta.schema.postgres.table.PostgresTable;

import java.util.List;

@Slf4j
public class PostgresSchemaService extends AbstractSchemaService {

    private final String configName;
    private final PostgresMetadataReader metadataReader;

    public PostgresSchemaService(String configName) {
        this.configName = configName;
        this.metadataReader = new PostgresMetadataReader(configName);
        registerTypes();
    }

    @Override
    public Table getTable(List<String> parents, String tableName) throws Exception {
        log.debug("Getting table: parents={}, tableName={}", parents, tableName);

        List<Column> columns = metadataReader.readTableColumns(parents, tableName);
        if (columns.isEmpty()) {
            log.warn("Table not found or has no columns: {}", tableName);
            return null;
        }

        List<String> qualifiedName = buildQualifiedName(parents, tableName);
        return new PostgresTable(qualifiedName, columns);
    }

    private List<String> buildQualifiedName(List<String> parents, String tableName) {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        builder.addAll(parents);
        builder.add(tableName);
        return builder.build();
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
