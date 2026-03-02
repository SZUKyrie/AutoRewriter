package org.autorewriter.rewriter.rule.util;

import java.util.Objects;

/**
 * Immutable reference to a column, identified by table name and column name.
 * Stable across operators regardless of positional index shifts.
 */
public final class ColumnRef {

    private final String tableName;
    private final String columnName;

    public ColumnRef(String tableName, String columnName) {
        this.tableName = tableName;
        this.columnName = columnName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnRef)) return false;
        ColumnRef that = (ColumnRef) o;
        return Objects.equals(tableName, that.tableName)
                && Objects.equals(columnName, that.columnName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, columnName);
    }

    @Override
    public String toString() {
        return tableName + "." + columnName;
    }
}

