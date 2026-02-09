package org.autorewriter.common.entity.postgres;

import lombok.Getter;
import org.autorewriter.common.entity.AbstractColumnDataType;
import org.autorewriter.common.entity.ColumnDataType;

import static org.autorewriter.common.constant.DataTypeConstants.COLUMN_MAP_DATA_TYPE_NAME;

/**
 * Map data type that has key and value
 *
 * @author wangyanjing <wangyanjing@kuaishou.com>
 * Created on 2024-07-09
 */
@Getter
public class ColumnMapDataType extends AbstractColumnDataType {

    private final ColumnBasicDataType keyType;
    private final ColumnDataType valueType;

    public ColumnMapDataType(boolean nullable, ColumnBasicDataType keyType, ColumnDataType valueType) {
        super(COLUMN_MAP_DATA_TYPE_NAME, nullable);
        this.keyType = keyType;
        this.valueType = valueType;
    }
}
