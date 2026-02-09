package org.autorewriter.common.converter;

import com.google.common.base.Preconditions;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.autorewriter.common.entity.ColumnDataType;
import org.autorewriter.common.enums.TableEngine;

import java.util.HashMap;
import java.util.Map;

import static org.autorewriter.common.constant.DataTypeConstants.UNKNOWN_DATA_TYPE_NAME;

public class ColumnDataTypeConverterRegistry {
    private static final Map<TableEngine, Map<Class<? extends ColumnDataType>, ColumnDataTypeConverter>> REGISTRY_BY_CLASS = new HashMap<>();
    private static final Map<TableEngine, Map<String, ColumnDataTypeConverter>> REGISTRY_BY_TYPE_NAME = new HashMap<>();

    static {
        Map<Class<? extends ColumnDataType>, ColumnDataTypeConverter> converterMap = new HashMap<>();
        //TODO: register more converters for different table engines
    }

    public static ColumnDataTypeConverter getConverter(TableEngine tableEngine, Class<? extends ColumnDataType> clazz,
                                                       RelDataTypeFactory typeFactory) {
        ColumnDataTypeConverter columnDataTypeConverter = REGISTRY_BY_CLASS.getOrDefault(tableEngine, new HashMap<>()).get(clazz);
        Preconditions.checkNotNull(columnDataTypeConverter, "could not find converter for %s with input data type %s", tableEngine.name(),
                clazz.getCanonicalName());
        columnDataTypeConverter.setTypeFactory(typeFactory);
        Preconditions.checkState(columnDataTypeConverter.supports(clazz),
                "converter %s doesn't support %s, " + "converter registry is registered wrongly.", columnDataTypeConverter, clazz.getCanonicalName());
        return columnDataTypeConverter;
    }

    public static ColumnDataTypeConverter getConverter(TableEngine tableEngine, String typeName) {
        Map<String, ColumnDataTypeConverter> typeString2DataTypeConverterMap = REGISTRY_BY_TYPE_NAME.getOrDefault(tableEngine, new HashMap<>());
        ColumnDataTypeConverter columnDataTypeConverter = typeString2DataTypeConverterMap.get(typeName.toUpperCase());
        if (columnDataTypeConverter == null) {
            columnDataTypeConverter = typeString2DataTypeConverterMap.get(UNKNOWN_DATA_TYPE_NAME);
        }
        Preconditions.checkNotNull(columnDataTypeConverter, "no default columnDataTypeConverter from unknown data type name for table engine %s",
                tableEngine.name());
        return columnDataTypeConverter;
    }
}
