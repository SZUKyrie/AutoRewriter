package org.autorewriter.meta.schema.postgres;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeImpl;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.type.SqlTypeName;
import org.autorewriter.common.entity.Column;
import org.autorewriter.meta.schema.AbstractSchemaService;
import org.autorewriter.meta.schema.postgres.table.PostgresTable;

import javax.sql.DataSource;
import java.util.List;

/**
 * PostgreSQL Schema 服务（基于 JDBC DataSource）
 * 参考 adaptiveengine 的 H2SchemaService 设计
 *
 * 支持两种模式：
 * 1. 通过连接配置名称获取元数据（使用 PostgresMetadataReader）
 * 2. 通过 DataSource 直接访问（用于测试）
 *
 * @author AutoRewriter
 * Created on 2026-02-13
 */
@Slf4j
public class PostgresJdbcSchemaService extends AbstractSchemaService {

    private final DataSource dataSource;
    private final RelDataTypeFactory typeFactory;
    private final String configName;
    private final PostgresMetadataReader metadataReader;

    /**
     * 构造函数（使用 DataSource）
     *
     * @param dataSource JDBC DataSource
     * @param typeFactory 类型工厂
     */
    public PostgresJdbcSchemaService(DataSource dataSource, RelDataTypeFactory typeFactory) {
        this.dataSource = dataSource;
        this.typeFactory = typeFactory;
        this.configName = null;
        this.metadataReader = null;
        registerTypes();
    }

    /**
     * 构造函数（使用配置名称）
     *
     * @param configName 配置名称
     * @param typeFactory 类型工厂
     */
    public PostgresJdbcSchemaService(String configName, RelDataTypeFactory typeFactory) {
        this.configName = configName;
        this.typeFactory = typeFactory;
        this.dataSource = null;
        this.metadataReader = new PostgresMetadataReader(configName);
        registerTypes();
    }

    @Override
    public Table getTable(List<String> parents, String tableName) throws Exception {
        log.debug("Getting table: parents={}, tableName={}", parents, tableName);

        if (metadataReader != null) {
            // 使用 MetadataReader 模式
            List<Column> columns = metadataReader.readTableColumns(parents, tableName);
            if (columns.isEmpty()) {
                log.warn("Table not found or has no columns: {}", tableName);
                return null;
            }

            List<String> qualifiedName = buildQualifiedName(parents, tableName);
            return new PostgresTable(qualifiedName, columns);
        } else {
            // 使用 DataSource 模式（通过 JdbcSchema）
            // 这里可以通过 JDBC 直接查询表结构
            log.debug("Using DataSource mode to get table: {}", tableName);
            // TODO: 可以在这里实现通过 DataSource 直接查询表结构的逻辑
            return null;
        }
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
        registerType("decimal", RelDataTypeImpl.proto(SqlTypeName.DECIMAL, 10, 2, true));
        registerType("boolean", RelDataTypeImpl.proto(SqlTypeName.BOOLEAN, true));
        registerType("date", RelDataTypeImpl.proto(SqlTypeName.DATE, true));
        registerType("time", RelDataTypeImpl.proto(SqlTypeName.TIME, true));
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public RelDataTypeFactory getTypeFactory() {
        return typeFactory;
    }
}

