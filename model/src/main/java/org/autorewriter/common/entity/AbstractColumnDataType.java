package org.autorewriter.common.entity;

import com.google.common.base.Preconditions;

public abstract class AbstractColumnDataType implements ColumnDataType {

    private final String typeName;
    private final boolean nullable;

    public AbstractColumnDataType(String typeName, boolean nullable) {
        Preconditions.checkNotNull(typeName, "type name is null");
        this.typeName = typeName.toUpperCase();
        this.nullable = nullable;
    }

    @Override
    public String getTypeName() {
        return this.typeName;
    }

    @Override
    public boolean isNullable() {
        return this.nullable;
    }
}
