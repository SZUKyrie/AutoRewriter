package org.autorewriter.common.utils;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.autorewriter.common.converter.ColumnDataTypeConverter;
import org.autorewriter.common.converter.ColumnDataTypeConverterRegistry;
import org.autorewriter.common.entity.ColumnDataType;
import org.autorewriter.common.entity.postgres.ColumnArrayDataType;
import org.autorewriter.common.entity.postgres.ColumnBasicDataType;
import org.autorewriter.common.entity.postgres.ColumnMapDataType;
import org.autorewriter.common.entity.postgres.ColumnStructDataType;
import org.autorewriter.common.enums.TableEngine;

import java.util.ArrayList;
import java.util.List;

public class DataTypeUtils {
    public static RelDataType fromColumnDataType(
            ColumnDataType columnDataType,
            TableEngine tableEngine,
            RelDataTypeFactory typeFactory
    ) {
        ColumnDataTypeConverter converter = ColumnDataTypeConverterRegistry.getConverter(tableEngine, columnDataType.getClass(), typeFactory);
        return converter.convert(columnDataType);
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
