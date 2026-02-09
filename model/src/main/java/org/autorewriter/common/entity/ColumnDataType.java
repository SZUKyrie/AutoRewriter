package org.autorewriter.common.entity;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.type.SqlTypeName;

import static org.autorewriter.common.constant.DataTypeConstants.PRECISION_NOT_SPECIFIED;
import static org.autorewriter.common.constant.DataTypeConstants.SCALE_NOT_SPECIFIED;

public interface ColumnDataType {

    /**
     * The string name of this data type
     * conforms to {@link SqlTypeName#name()}
     *
     * @return string name of this data type
     */
    String getTypeName();

    /**
     * The precision of this data type
     * conforms to {@link RelDataType#getPrecision()}
     *
     * @return precision
     */
    default int getPrecision() {
        return PRECISION_NOT_SPECIFIED;
    }

    /**
     * The scale of this data type
     * conforms to {@link RelDataType#getScale()}
     *
     * @return scale
     */
    default int getScale() {
        return SCALE_NOT_SPECIFIED;
    }

    /**
     * Whether this type allows null values
     * conforms to {@link RelDataType#isNullable()}
     *
     * @return allows null values or not
     */
    boolean isNullable();
}
