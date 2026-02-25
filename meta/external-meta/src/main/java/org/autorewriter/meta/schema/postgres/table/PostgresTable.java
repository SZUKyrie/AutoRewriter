package org.autorewriter.meta.schema.postgres.table;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.Statistics;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.util.ImmutableBitSet;
import org.autorewriter.common.entity.Column;
import org.autorewriter.common.entity.ColumnDataType;
import org.autorewriter.common.enums.TableEngine;
import org.autorewriter.common.utils.DataTypeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostgresTable extends AbstractTable {

    @NotNull
    private List<String> qualifiedName;

    @NotNull
    private List<Column> columnList;

    /**
     * Each ImmutableBitSet represents one unique/primary key:
     * the set contains the 0-based column indices that form that key.
     * Empty list means no unique key information is available.
     */
    private final List<ImmutableBitSet> uniqueKeys;

    /** Backward-compatible constructor: no unique key info. */
    public PostgresTable(List<String> qualifiedName, List<Column> columnList) {
        this(qualifiedName, columnList, Collections.emptyList());
    }

    public PostgresTable(List<String> qualifiedName, List<Column> columnList,
                         List<ImmutableBitSet> uniqueKeys) {
        this.qualifiedName = qualifiedName;
        this.columnList = columnList;
        this.uniqueKeys = uniqueKeys;
    }

    @Override
    public Statistic getStatistic() {
        return Statistics.of(null, uniqueKeys, null, null);
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
