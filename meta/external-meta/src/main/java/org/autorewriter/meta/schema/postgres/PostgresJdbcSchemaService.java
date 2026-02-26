package org.autorewriter.meta.schema.postgres;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.type.*;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.Util;
import org.apache.calcite.util.ImmutableBitSet;
import org.autorewriter.common.constant.DataTypeConstants;
import org.autorewriter.common.entity.Column;
import org.autorewriter.common.entity.ColumnDataType;
import org.autorewriter.common.entity.PrecisionScale;
import org.autorewriter.common.utils.DataTypeUtils;
import org.autorewriter.meta.schema.AbstractSchemaService;
import org.autorewriter.meta.schema.postgres.table.PostgresTable;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.autorewriter.common.constant.DataTypeConstants.COLUMN_ARRAY_DATA_TYPE_NAME;
import static org.autorewriter.common.constant.JdbcConstants.*;
import static org.autorewriter.common.constant.NotationConstants.OPEN_PARENTHESIS;


@Slf4j
public class PostgresJdbcSchemaService extends AbstractSchemaService {

    private final DataSource dataSource;
    private RelDataTypeFactory relDataTypeFactory;

    public PostgresJdbcSchemaService(DataSource dataSource,
                                     RelDataTypeFactory relDataTypeFactory) {
        this.dataSource = dataSource;
        this.relDataTypeFactory = relDataTypeFactory;
    }

    @Override
    public Table getTable(List<String> parents, String tableName) throws Exception {
        // PostgreSQL stores unquoted identifiers in lowercase
        String catalog = parents.isEmpty() ? null : parents.get(0).toLowerCase();
        String schema = parents.size() > 1 ? parents.get(1).toLowerCase() : "public";
        tableName = tableName.toLowerCase();

        RelProtoDataType relProtoDataType = getRelDataType(catalog, schema, tableName);
        RelDataType relDataType = relProtoDataType.apply(this.relDataTypeFactory);
        List<Column> fieldList = new ArrayList<>();
        List<RelDataTypeField> relFieldList = relDataType.getFieldList();
        for (RelDataTypeField relDataTypeField : relFieldList) {
            Column column = new Column();
            column.setName(relDataTypeField.getName().toLowerCase());
            ColumnDataType columnDataType = DataTypeUtils.createColumnDataTypeFromRelDataType(relDataTypeField.getType());
            column.setType(columnDataType);
            fieldList.add(column);
        }
        List<String> qualifiedTableName = new ArrayList<>(parents);
        qualifiedTableName.add(tableName);

        Map<String, Integer> colIndexMap = new java.util.HashMap<>();
        for (int i = 0; i < fieldList.size(); i++) {
            colIndexMap.put(fieldList.get(i).getName().toLowerCase(), i);
        }
        List<ImmutableBitSet> uniqueKeys = readUniqueKeys(schema, tableName, colIndexMap);

        return new PostgresTable(qualifiedTableName, fieldList, uniqueKeys);
    }

    private List<ImmutableBitSet> readUniqueKeys(String schema, String tableName,
                                                  Map<String, Integer> colIndexMap) {
        List<ImmutableBitSet> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();

            // Primary key
            try (ResultSet pk = meta.getPrimaryKeys(null, schema, tableName)) {
                List<Integer> pkCols = new ArrayList<>();
                while (pk.next()) {
                    Integer idx = colIndexMap.get(pk.getString("COLUMN_NAME").toLowerCase());
                    if (idx != null) pkCols.add(idx);
                }
                if (!pkCols.isEmpty()) result.add(ImmutableBitSet.of(pkCols));
            }

            // Unique indexes
            try (ResultSet idxRs = meta.getIndexInfo(null, schema, tableName, true, false)) {
                java.util.Map<String, List<Integer>> idxCols = new java.util.LinkedHashMap<>();
                while (idxRs.next()) {
                    if (idxRs.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) continue;
                    String idxName = idxRs.getString("INDEX_NAME");
                    String col = idxRs.getString("COLUMN_NAME");
                    if (idxName == null || col == null) continue;
                    Integer idx = colIndexMap.get(col.toLowerCase());
                    if (idx != null) idxCols.computeIfAbsent(idxName, k -> new ArrayList<>()).add(idx);
                }
                for (List<Integer> cols : idxCols.values()) {
                    ImmutableBitSet key = ImmutableBitSet.of(cols);
                    if (!result.contains(key)) result.add(key);
                }
            }
        } catch (Exception e) {
            log.warn("readUniqueKeys: failed for {}.{}: {}", schema, tableName, e.getMessage());
        }
        return result;
    }

    RelProtoDataType getRelDataType(String catalogName, String schemaName, String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return getRelDataType(metaData, catalogName, schemaName, tableName);
        }
    }

    private RelProtoDataType getRelDataType(DatabaseMetaData metaData, String catalogName, String schemaName, String tableName) throws SQLException {
        log.debug("getRelDataType: catalog={}, schema={}, table={}", catalogName, schemaName, tableName);
        final ResultSet resultSet = metaData.getColumns(null, schemaName, tableName, null);
        final RelDataTypeFactory.Builder fieldInfo = this.relDataTypeFactory.builder();
        while (resultSet.next()) {
            final String columnName = requireNonNull(resultSet.getString(COLUMN_NAME), "columnName");
            final int dataType = resultSet.getInt(DATA_TYPE);
            final String typeString = resultSet.getString(TYPE_NAME);
            final int precision;
            final int scale;
            switch (dataType) {
                case Types.TIMESTAMP:
                case Types.TIME:
                    precision = resultSet.getInt(DECIMAL_DIGITS);
                    scale = 0;
                    break;
                default:
                    precision = resultSet.getInt(COLUMN_SIZE);
                    scale = resultSet.getInt(DECIMAL_DIGITS);
                    break;
            }
            RelDataType sqlType = sqlType(this.relDataTypeFactory, dataType, precision, scale, typeString);
            boolean nullable = resultSet.getInt(NULLABLE) != DatabaseMetaData.columnNoNulls;
            fieldInfo.add(columnName, sqlType).nullable(nullable);
        }
        resultSet.close();
        return RelDataTypeImpl.proto(fieldInfo.build());
    }

    private static RelDataType sqlType(RelDataTypeFactory typeFactory, int dataType, int precision, int scale, @Nullable String typeString) {
        // Fall back to ANY if type is unknown
        final SqlTypeName sqlTypeName = Util.first(SqlTypeName.getNameForJdbcType(dataType), SqlTypeName.ANY);
        switch (sqlTypeName) {
            case ARRAY:
                RelDataType component = null;
                String arrayTypeStringSuffix = " " + COLUMN_ARRAY_DATA_TYPE_NAME;
                if (typeString != null && typeString.endsWith(arrayTypeStringSuffix)) {
                    // E.g. hsqldb gives "INTEGER ARRAY", so we deduce the component type
                    // "INTEGER".
                    final String remaining = typeString.substring(0, typeString.length() - arrayTypeStringSuffix.length());
                    component = parseTypeString(typeFactory, remaining);
                }
                if (component == null) {
                    component = typeFactory.createTypeWithNullability(typeFactory.createSqlType(SqlTypeName.ANY), true);
                }
                return typeFactory.createArrayType(component, DataTypeConstants.MAX_CARDINALITY_OF_ARRAY_TYPE);
            default:
                break;
        }
        if (precision >= 0 && scale >= 0 && sqlTypeName.allowsPrecScale(true, true)) {
            return typeFactory.createSqlType(sqlTypeName, precision, scale);
        } else if (precision >= 0 && sqlTypeName.allowsPrecNoScale()) {
            return typeFactory.createSqlType(sqlTypeName, precision);
        } else {
            assert sqlTypeName.allowsNoPrecNoScale();
            return typeFactory.createSqlType(sqlTypeName);
        }
    }

    /**
     * Given "INTEGER", returns BasicSqlType(INTEGER).
     * Given "VARCHAR(10)", returns BasicSqlType(VARCHAR, 10).
     * Given "NUMERIC(10, 2)", returns BasicSqlType(NUMERIC, 10, 2).
     */
    private static RelDataType parseTypeString(RelDataTypeFactory typeFactory, String typeString) {
        PrecisionScale precisionScale = DataTypeUtils.parsePrecisionAndScale(typeString);
        int precision = precisionScale.getPrecision();
        int scale = precisionScale.getScale();
        int open = typeString.indexOf(OPEN_PARENTHESIS);
        if (open >= 0) {
            typeString = typeString.substring(0, open);
        }
        try {
            final SqlTypeName typeName = SqlTypeName.valueOf(typeString);
            return typeName.allowsPrecScale(true, true) ? typeFactory.createSqlType(typeName, precision, scale) : typeName.allowsPrecScale(true,
                    false) ? typeFactory.createSqlType(typeName, precision) : typeFactory.createSqlType(typeName);
        } catch (IllegalArgumentException e) {
            return typeFactory.createTypeWithNullability(typeFactory.createSqlType(SqlTypeName.ANY), true);
        }
    }
}

