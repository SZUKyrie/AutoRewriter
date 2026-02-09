package org.autorewriter.common.converter;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.autorewriter.common.entity.ColumnDataType;

public interface ColumnDataTypeConverter {

    /**
     * Whether this converter supports conversion from instance of clazz
     */
    boolean supports(Class<? extends ColumnDataType> clazz);

    RelDataType convert(ColumnDataType columnDataType);

    ColumnDataType fromTypeString(String dataTypeString, boolean nullable);

    void setTypeFactory(RelDataTypeFactory typeFactory);

    String toTypeString(ColumnDataType columnDataType);
}