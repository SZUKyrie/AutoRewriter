package org.autorewriter.common.converter.postgres;

import com.google.common.base.Preconditions;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.type.SqlTypeName;
import org.autorewriter.common.constant.DataTypeConstants;
import org.autorewriter.common.constant.NotationConstants;
import org.autorewriter.common.converter.AbstractColumnDataTypeConverter;
import org.autorewriter.common.entity.ColumnDataType;
import org.autorewriter.common.entity.PrecisionScale;
import org.autorewriter.common.entity.postgres.ColumnBasicDataType;
import org.autorewriter.common.enums.TableEngine;
import org.autorewriter.common.utils.DataTypeUtils;

import java.util.HashMap;
import java.util.Map;

import static org.autorewriter.common.constant.DataTypeConstants.*;
import static org.autorewriter.common.constant.NotationConstants.OPEN_PARENTHESIS;

/**
 * Converter that converts {@link ColumnBasicDataType} to calcite's {@link RelDataType}
 * 完全参考 adaptiveengine 的 Hive ColumnBasicDataTypeConverter 实现
 *
 * @author AutoRewriter
 * Created on 2026-02-13
 */
public class ColumnBasicDataTypeConverter extends AbstractColumnDataTypeConverter {

    /*
     * Alias to data type name mapping, such as int -> integer, string -> varchar
     */
    private static final Map<String, String> ALIAS_TO_TYPE_NAME = new HashMap<>();

    /**
     * Alias to data type name mapping reverse, such as integer -> int, varchar -> string
     */
    private static final Map<String, String> SQL_TYPE_TO_TYPE_NAME = new HashMap<>();

    /*
     * Alias to precision and scale mapping, such as string has int_max precision implicitly
     */
    private static final Map<String, PrecisionScale> ALIAS_TO_PRECISION_SCALE = new HashMap<>();

    static {
        ALIAS_TO_TYPE_NAME.put(INT_DATA_TYPE_NAME, SqlTypeName.INTEGER.getName());
        ALIAS_TO_TYPE_NAME.put(NUMERIC_DATA_TYPE_NAME, SqlTypeName.DECIMAL.getName());
        ALIAS_TO_TYPE_NAME.put(STRING_DATA_TYPE_NAME, SqlTypeName.VARCHAR.getName());

        SQL_TYPE_TO_TYPE_NAME.put(SqlTypeName.INTEGER.getName(), INT_DATA_TYPE_NAME);
        SQL_TYPE_TO_TYPE_NAME.put(SqlTypeName.TIMESTAMP.getName(), TIMESTAMP_DATA_TYPE_NAME);

        ALIAS_TO_PRECISION_SCALE.put(STRING_DATA_TYPE_NAME, new PrecisionScale(Integer.MAX_VALUE, SCALE_NOT_SPECIFIED));
    }

    @Override
    public boolean supports(Class<? extends ColumnDataType> clazz) {
        return clazz == ColumnBasicDataType.class;
    }

    @Override
    public ColumnDataType fromTypeString(String dataTypeString, boolean nullable) {
        PrecisionScale precisionScale = DataTypeUtils.parsePrecisionAndScale(dataTypeString);
        String typeName = dataTypeString;
        int open = dataTypeString.indexOf(OPEN_PARENTHESIS);
        if (open > 0) {
            typeName = dataTypeString.substring(0, open);
        }
        return new ColumnBasicDataType(typeName, nullable, precisionScale.getPrecision(),
                precisionScale.getScale());
    }

    @Override
    public String toTypeString(ColumnDataType columnDataType) {
        String typeName = columnDataType.getTypeName().toUpperCase();
        if (isStringType(columnDataType)) {
            return STRING_DATA_TYPE_NAME;
        }
        SqlTypeName sqlTypeName = SqlTypeName.get(typeName);
        Preconditions.checkState(sqlTypeName != null || ALIAS_TO_TYPE_NAME.containsKey(typeName),
                "Unrecognized type name %s for table engine %s", typeName, TableEngine.POSTGRESQL.name());
        if (sqlTypeName == null) {
            typeName = ALIAS_TO_TYPE_NAME.get(typeName);
            sqlTypeName = SqlTypeName.get(typeName);
        }

        String pgTypeName = SQL_TYPE_TO_TYPE_NAME.get(typeName.toUpperCase());
        if (pgTypeName != null) {
            return pgTypeName;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(typeName);
        if (sqlTypeName.allowsPrec() && columnDataType.getPrecision() != DataTypeConstants.PRECISION_NOT_SPECIFIED) {
            builder.append(NotationConstants.OPEN_PARENTHESIS).append(columnDataType.getPrecision());
            if (sqlTypeName.allowsScale() && columnDataType.getScale() != DataTypeConstants.SCALE_NOT_SPECIFIED) {
                builder.append(NotationConstants.COMMA).append(columnDataType.getScale());
            }
            builder.append(NotationConstants.CLOSE_PARENTHESIS);
        }
        return builder.toString();
    }

    private boolean isStringType(ColumnDataType columnDataType) {
        String typeName = columnDataType.getTypeName();
        if (STRING_DATA_TYPE_NAME.equals(typeName)) {
            return true;
        }
        SqlTypeName sqlTypeName = SqlTypeName.get(typeName);
        return sqlTypeName == SqlTypeName.VARCHAR && columnDataType.getPrecision() == Integer.MAX_VALUE;
    }

    @Override
    public RelDataType convertSafely(ColumnDataType columnDataType) {
        ColumnBasicDataType columnBasicDataType = (ColumnBasicDataType) columnDataType;
        String typeName = columnBasicDataType.getTypeName().toUpperCase();
        SqlTypeName sqlTypeName = SqlTypeName.get(typeName);
        Preconditions.checkState(sqlTypeName != null || ALIAS_TO_TYPE_NAME.containsKey(typeName),
                "Unrecognized type name %s for table engine %s", typeName, TableEngine.POSTGRESQL.name());
        PrecisionScale precisionScale = null;
        if (sqlTypeName == null) {
            precisionScale = ALIAS_TO_PRECISION_SCALE.get(typeName);
            typeName = ALIAS_TO_TYPE_NAME.get(typeName);
            sqlTypeName = SqlTypeName.get(typeName);
        }
        if (precisionScale != null) {
            columnBasicDataType = new ColumnBasicDataType(typeName, columnDataType.isNullable(),
                    precisionScale.getPrecision(), precisionScale.getScale());
        }
        RelDataType type;
        if (sqlTypeName.allowsPrecScale(true, true)) {
            type = relDataTypeFactory.createSqlType(sqlTypeName, columnBasicDataType.getPrecision(),
                    columnBasicDataType.getScale());
        } else if (sqlTypeName.allowsPrec()) {
            type = relDataTypeFactory.createSqlType(sqlTypeName, columnBasicDataType.getPrecision());
        } else {
            type = relDataTypeFactory.createSqlType(sqlTypeName);
        }
        type = relDataTypeFactory.createTypeWithNullability(type, columnBasicDataType.isNullable());
        return type;
    }
}

