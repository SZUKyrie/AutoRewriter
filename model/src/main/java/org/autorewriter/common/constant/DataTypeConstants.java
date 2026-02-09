package org.autorewriter.common.constant;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.type.SqlTypeName;

public class DataTypeConstants {

    public static final int SCALE_NOT_SPECIFIED = RelDataType.SCALE_NOT_SPECIFIED;
    public static final int PRECISION_NOT_SPECIFIED = RelDataType.PRECISION_NOT_SPECIFIED;
    // -1 means unlimited
    public static final int MAX_CARDINALITY_OF_ARRAY_TYPE = -1;
    public static final String COLUMN_MAP_DATA_TYPE_NAME = SqlTypeName.MAP.getName();
    public static final String COLUMN_ARRAY_DATA_TYPE_NAME = SqlTypeName.ARRAY.getName();
    public static final String INT_DATA_TYPE_NAME = "INT";
    public static final String TIMESTAMP_DATA_TYPE_NAME = "TIMESTAMP";
    public static final String NUMERIC_DATA_TYPE_NAME = "NUMERIC";
    public static final String STRING_DATA_TYPE_NAME = "STRING";
    public static final String STRUCT_DATA_TYPE_NAME = "STRUCT";
    public static final String UNKNOWN_DATA_TYPE_NAME = "UNKNOWN_DATA_TYPE_NAME";
}
