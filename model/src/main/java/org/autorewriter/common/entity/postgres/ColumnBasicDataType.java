package org.autorewriter.common.entity.postgres;


import org.autorewriter.common.entity.AbstractColumnDataType;

import static org.autorewriter.common.constant.DataTypeConstants.PRECISION_NOT_SPECIFIED;
import static org.autorewriter.common.constant.DataTypeConstants.SCALE_NOT_SPECIFIED;

/**
 * Basic data type of column, including int, decimal, string etc.
 *
 * @author wangyanjing <wangyanjing@kuaishou.com>
 * Created on 2024-07-09
 */
public class ColumnBasicDataType extends AbstractColumnDataType {

    private int precision = PRECISION_NOT_SPECIFIED;
    private int scale = SCALE_NOT_SPECIFIED;

    public ColumnBasicDataType(String typeName, boolean nullable) {
        super(typeName, nullable);
    }

    public ColumnBasicDataType(String typeName, boolean nullable, int precision) {
        super(typeName, nullable);
        this.precision = precision;
    }

    public ColumnBasicDataType(String typeName, boolean nullable, int precision, int scale) {
        super(typeName, nullable);
        this.precision = precision;
        this.scale = scale;
    }

    @Override
    public int getPrecision() {
        return this.precision;
    }

    @Override
    public int getScale() {
        return this.scale;
    }
}
