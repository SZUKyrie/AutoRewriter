package org.autorewriter.common.entity.postgres;


import lombok.Getter;
import org.autorewriter.common.entity.AbstractColumnDataType;
import org.autorewriter.common.entity.ColumnDataType;

import static org.autorewriter.common.constant.DataTypeConstants.COLUMN_ARRAY_DATA_TYPE_NAME;


@Getter
public class ColumnArrayDataType extends AbstractColumnDataType {

    private final ColumnDataType elementType;

    public ColumnArrayDataType(boolean nullable, ColumnDataType elementType) {
        super(COLUMN_ARRAY_DATA_TYPE_NAME, nullable);
        this.elementType = elementType;
    }
}
