package org.autorewriter.common.entity.postgres;

import lombok.Getter;
import org.autorewriter.common.entity.AbstractColumnDataType;
import org.autorewriter.common.entity.ColumnDataType;

import java.util.List;

import static org.autorewriter.common.constant.DataTypeConstants.STRUCT_DATA_TYPE_NAME;

/**
 * Struct data type that has a list of column data type and name
 *
 * @author wangyanjing <wangyanjing@kuaishou.com>
 * Created on 2024-07-09
 */
@Getter
public class ColumnStructDataType extends AbstractColumnDataType {

    private final List<String> columnNames;
    private final List<ColumnDataType> columnDataTypes;

    public ColumnStructDataType(boolean nullable, List<String> columnNames, List<ColumnDataType> columnDataTypes) {
        super(STRUCT_DATA_TYPE_NAME, nullable);
        this.columnNames = columnNames;
        this.columnDataTypes = columnDataTypes;
    }
}
