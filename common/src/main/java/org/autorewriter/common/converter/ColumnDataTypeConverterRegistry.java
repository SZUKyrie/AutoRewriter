package org.autorewriter.common.converter;

import com.google.common.base.Preconditions;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.autorewriter.common.converter.postgres.ColumnBasicDataTypeConverter;
import org.autorewriter.common.entity.ColumnDataType;
import org.autorewriter.common.entity.postgres.ColumnBasicDataType;
import org.autorewriter.common.enums.TableEngine;

import java.util.HashMap;
import java.util.Map;

import static org.autorewriter.common.constant.DataTypeConstants.UNKNOWN_DATA_TYPE_NAME;

/**
 * A registry centre that all converters are registered.
 *
 * PostgreSQL 只需要基本类型转换器，因为：
 * 1. PostgresMetadataReader 通过 JDBC 只读取基本类型
 * 2. 不需要 Hive 的复杂类型（Map, Array, Struct）
 *
 * @author AutoRewriter
 * Created on 2026-02-13
 */
public class ColumnDataTypeConverterRegistry {
    private static final Map<TableEngine, Map<Class<? extends ColumnDataType>, ColumnDataTypeConverter>> REGISTRY_BY_CLASS = new HashMap<>();
    private static final Map<TableEngine, Map<String, ColumnDataTypeConverter>> REGISTRY_BY_TYPE_NAME = new HashMap<>();

    static {
        // Register PostgreSQL converters - 只需要基本类型转换器
        Map<Class<? extends ColumnDataType>, ColumnDataTypeConverter> postgresConverterMap = new HashMap<>();
        ColumnBasicDataTypeConverter basicConverter = new ColumnBasicDataTypeConverter();
        postgresConverterMap.put(ColumnBasicDataType.class, basicConverter);
        REGISTRY_BY_CLASS.put(TableEngine.POSTGRESQL, postgresConverterMap);

        Map<String, ColumnDataTypeConverter> postgresTypeName2ConverterMap = new HashMap<>();
        // 所有未知类型都使用基本类型转换器
        postgresTypeName2ConverterMap.put(UNKNOWN_DATA_TYPE_NAME, basicConverter);
        REGISTRY_BY_TYPE_NAME.put(TableEngine.POSTGRESQL, postgresTypeName2ConverterMap);
    }

    /**
     * Get a {@link ColumnDataTypeConverter} that its input data type belongs to the specific
     * table engine and is an instance of the clazz
     * @param tableEngine the converter's input data type belongs to
     * @param clazz the input data type's clazz
     * @param typeFactory used by {@link ColumnDataTypeConverter} to create rel data type
     * @return a converter that can convert the table engine's column data type to
     * calcite's data type
     */
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

    /**
     * Get a {@link ColumnDataTypeConverter} that is used to convert data type string to {@link ColumnDataType}
     *
     * @param tableEngine the table engine that returned {@link ColumnDataTypeConverter} belongs to
     * @param typeName base type name, such as int, bigint, varchar etc.
     * @return {@link ColumnDataTypeConverter} without {@link RelDataTypeFactory} field set
     */
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
