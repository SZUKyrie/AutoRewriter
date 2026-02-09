package org.autorewriter.meta.schema.postgres.table;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.impl.AbstractTable;
import org.autorewriter.common.entity.Column;
import org.autorewriter.common.entity.ColumnDataType;
import org.autorewriter.common.enums.TableEngine;
import org.autorewriter.common.utils.DataTypeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PostgresTable extends AbstractTable {

    @NotNull
    private List<String> qualifiedName;

    @NotNull
    private List<Column> columnList;

    public PostgresTable(List<String> qualifiedName, List<Column> columnList) {
        this.qualifiedName = qualifiedName;
        this.columnList = columnList;
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        List<String> fieldNames = new ArrayList<>();
        List<RelDataType> fieldTypes = new ArrayList<>();
        for (Column column : columnList) {
            fieldNames.add(column.getName());
            ColumnDataType columnDataType = column.getType();
            RelDataType relDataType = DataTypeUtils.fromColumnDataType(columnDataType, TableEngine.POSTGRESQL, typeFactory);
            fieldTypes.add(relDataType);
        }
        return typeFactory.createStructType(fieldTypes, fieldNames);
    }
}
