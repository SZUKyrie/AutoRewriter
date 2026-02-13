package org.autorewriter.meta.schema.postgres;

import lombok.extern.slf4j.Slf4j;
import org.autorewriter.common.entity.Column;
import org.autorewriter.common.entity.postgres.ColumnBasicDataType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.autorewriter.common.constant.JdbcConstants.*;

/**
 * PostgreSQL metadata reader using JDBC
 */
@Slf4j
public class PostgresMetadataReader {

    private final String configName;

    public PostgresMetadataReader(String configName) {
        this.configName = configName;
    }

    /**
     * Read table metadata from PostgreSQL
     */
    public List<Column> readTableColumns(List<String> parents, String tableName) throws SQLException {
        List<Column> columns = new ArrayList<>();

        try (Connection connection = PostgresConnectionManager.getConnection(configName)) {
            DatabaseMetaData metaData = connection.getMetaData();

            String catalog = getCatalog(parents, connection);
            String schema = getSchema(parents);

            log.debug("Reading table: catalog={}, schema={}, table={}", catalog, schema, tableName);

            try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, null)) {
                while (rs.next()) {
                    String columnName = rs.getString(COLUMN_NAME);
                    String typeName = rs.getString(TYPE_NAME);
                    int columnSize = rs.getInt(COLUMN_SIZE);
                    int decimalDigits = rs.getInt(DECIMAL_DIGITS);
                    int nullable = rs.getInt(NULLABLE);

                    boolean isNullable = (nullable == DatabaseMetaData.columnNullable);

                    ColumnBasicDataType columnDataType = createColumnDataType(
                        typeName, isNullable, columnSize, decimalDigits);

                    Column column = new Column(columnName, columnDataType, false);
                    columns.add(column);

                    log.debug("Read column: name={}, type={}, size={}, nullable={}",
                        columnName, typeName, columnSize, isNullable);
                }
            }
        }

        if (columns.isEmpty()) {
            log.warn("No columns found for table: {}", tableName);
        }

        return columns;
    }

    /**
     * Check if a table exists
     */
    public boolean tableExists(List<String> parents, String tableName) throws SQLException {
        try (Connection connection = PostgresConnectionManager.getConnection(configName)) {
            DatabaseMetaData metaData = connection.getMetaData();

            String catalog = getCatalog(parents, connection);
            String schema = getSchema(parents);

            try (ResultSet rs = metaData.getTables(catalog, schema, tableName, new String[]{"TABLE"})) {
                return rs.next();
            }
        }
    }

    /**
     * List all tables in the schema
     */
    public List<String> listTables(List<String> parents) throws SQLException {
        List<String> tables = new ArrayList<>();

        try (Connection connection = PostgresConnectionManager.getConnection(configName)) {
            DatabaseMetaData metaData = connection.getMetaData();

            String catalog = getCatalog(parents, connection);
            String schema = getSchema(parents);

            try (ResultSet rs = metaData.getTables(catalog, schema, null, new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString(TABLE_NAME);
                    tables.add(tableName);
                }
            }
        }

        return tables;
    }

    private String getCatalog(List<String> parents, Connection connection) throws SQLException {
        if (parents.isEmpty()) {
            return connection.getCatalog();
        }
        return parents.get(0);
    }

    private String getSchema(List<String> parents) {
        PostgresConnectionConfig config = PostgresConnectionManager.getConfig(configName);
        if (config != null && config.getSchema() != null) {
            return config.getSchema();
        }

        if (parents.size() > 1) {
            return parents.get(1);
        }

        // Default to public schema
        return "public";
    }

    private ColumnBasicDataType createColumnDataType(String typeName, boolean nullable,
                                                      int columnSize, int decimalDigits) {
        String normalizedTypeName = normalizeTypeName(typeName);

        // Types that need precision
        if (needsPrecision(normalizedTypeName)) {
            if (needsScale(normalizedTypeName) && decimalDigits > 0) {
                return new ColumnBasicDataType(normalizedTypeName, nullable, columnSize, decimalDigits);
            } else if (columnSize > 0) {
                return new ColumnBasicDataType(normalizedTypeName, nullable, columnSize);
            }
        }

        return new ColumnBasicDataType(normalizedTypeName, nullable);
    }

    private String normalizeTypeName(String typeName) {
        if (typeName == null) {
            return "VARCHAR";
        }

        // Normalize PostgreSQL type names to standard SQL types
        String upperTypeName = typeName.toUpperCase();

        // Map PostgreSQL types to standard types
        switch (upperTypeName) {
            case "INT4":
                return "INTEGER";
            case "INT8":
                return "BIGINT";
            case "INT2":
                return "SMALLINT";
            case "FLOAT4":
                return "REAL";
            case "FLOAT8":
                return "DOUBLE";
            case "BOOL":
                return "BOOLEAN";
            case "TIMESTAMPTZ":
                return "TIMESTAMP";
            case "TIMETZ":
                return "TIME";
            default:
                return upperTypeName;
        }
    }

    private boolean needsPrecision(String typeName) {
        switch (typeName) {
            case "VARCHAR":
            case "CHAR":
            case "DECIMAL":
            case "NUMERIC":
                return true;
            default:
                return false;
        }
    }

    private boolean needsScale(String typeName) {
        switch (typeName) {
            case "DECIMAL":
            case "NUMERIC":
                return true;
            default:
                return false;
        }
    }
}

