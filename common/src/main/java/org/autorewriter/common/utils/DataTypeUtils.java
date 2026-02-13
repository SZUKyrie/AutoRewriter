package org.autorewriter.common.utils;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.autorewriter.common.converter.ColumnDataTypeConverter;
import org.autorewriter.common.converter.ColumnDataTypeConverterRegistry;
import org.autorewriter.common.entity.ColumnDataType;
import org.autorewriter.common.entity.PrecisionScale;
import org.autorewriter.common.entity.postgres.ColumnArrayDataType;
import org.autorewriter.common.entity.postgres.ColumnBasicDataType;
import org.autorewriter.common.entity.postgres.ColumnMapDataType;
import org.autorewriter.common.entity.postgres.ColumnStructDataType;
import org.autorewriter.common.enums.TableEngine;

import java.util.ArrayList;
import java.util.List;

import static org.autorewriter.common.constant.DataTypeConstants.*;
import static org.autorewriter.common.constant.NotationConstants.*;

public class DataTypeUtils {
    public static RelDataType fromColumnDataType(
            ColumnDataType columnDataType,
            TableEngine tableEngine,
            RelDataTypeFactory typeFactory
    ) {
        ColumnDataTypeConverter converter = ColumnDataTypeConverterRegistry.getConverter(tableEngine, columnDataType.getClass(), typeFactory);
        return converter.convert(columnDataType);
    }

    /**
     * Convert PostgreSQL type string to ColumnDataType
     * 参考 adaptiveengine 的 fromHiveTypeString 实现
     */
    public static ColumnDataType fromPostgresTypeString(String typeString, boolean nullable) {
        String baseTypeName = getBaseTypeName(typeString);
        ColumnDataTypeConverter converter = ColumnDataTypeConverterRegistry.getConverter(TableEngine.POSTGRESQL, baseTypeName);
        return converter.fromTypeString(typeString, nullable);
    }

    /**
     * Parse precision and scale from type string like VARCHAR(100) or DECIMAL(10,2)
     */
    public static PrecisionScale parsePrecisionAndScale(String typeString) {
        int openIndex = typeString.indexOf(OPEN_PARENTHESIS);
        if (openIndex < 0) {
            return new PrecisionScale(PRECISION_NOT_SPECIFIED, SCALE_NOT_SPECIFIED);
        }
        int closeIndex = typeString.indexOf(CLOSE_PARENTHESIS);
        String precisionScaleString = typeString.substring(openIndex + 1, closeIndex);
        String[] parts = precisionScaleString.split(COMMA);
        if (parts.length == 1) {
            return new PrecisionScale(Integer.parseInt(parts[0].trim()), SCALE_NOT_SPECIFIED);
        } else if (parts.length == 2) {
            return new PrecisionScale(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        }
        return new PrecisionScale(PRECISION_NOT_SPECIFIED, SCALE_NOT_SPECIFIED);
    }

    /**
     * Get base type name from type string
     * For example: map<int,string> -> map, array<int> -> array, struct<...> -> struct, int -> int
     */
    private static String getBaseTypeName(String typeString) {
        int openAngleBracketIndex = typeString.indexOf(OPEN_ANGLE_BRACKET);
        if (openAngleBracketIndex > 0) {
            return typeString.substring(0, openAngleBracketIndex).trim().toUpperCase();
        }
        int openParenthesisIndex = typeString.indexOf(OPEN_PARENTHESIS);
        if (openParenthesisIndex > 0) {
            return typeString.substring(0, openParenthesisIndex).trim().toUpperCase();
        }
        return typeString.trim().toUpperCase();
    }

    public static ColumnDataType createColumnDataTypeFromRelDataType(RelDataType relDataType) {

        ColumnDataType columnDataType = null;
        if (relDataType.isStruct()) {
            List<RelDataTypeField> fieldList = relDataType.getFieldList();
            List<ColumnDataType> fieldColumnDataTypes = new ArrayList<>();
            for (RelDataTypeField relDataTypeField : fieldList) {
                fieldColumnDataTypes.add(createColumnDataTypeFromRelDataType(relDataTypeField.getType()));
            }
            columnDataType = new ColumnStructDataType(relDataType.isNullable(), relDataType.getFieldNames(), fieldColumnDataTypes);
        } else {
            switch (relDataType.getSqlTypeName()) {
                case ARRAY:
                    RelDataType componentType = relDataType.getComponentType();
                    columnDataType = new ColumnArrayDataType(relDataType.isNullable(), createColumnDataTypeFromRelDataType(componentType));
                    break;

                case MAP:
                    RelDataType keyType = relDataType.getKeyType();
                    RelDataType valueType = relDataType.getValueType();
                    columnDataType = new ColumnMapDataType(relDataType.isNullable(),
                            (ColumnBasicDataType) createColumnDataTypeFromRelDataType(keyType), createColumnDataTypeFromRelDataType(valueType));
                    break;
                default:
                    columnDataType = new ColumnBasicDataType(relDataType.getSqlTypeName().getName(), relDataType.isNullable(),
                            relDataType.getPrecision(), relDataType.getScale());
                    break;
            }
        }
        return columnDataType;
    }
}
