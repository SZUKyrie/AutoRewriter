package org.autorewriter.common.converter;

import com.google.common.base.Preconditions;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.autorewriter.common.entity.ColumnDataType;

/**
 * Convert DB's ColumnDataType to CaRelDataType
 * */
public abstract class AbstractColumnDataTypeConverter implements ColumnDataTypeConverter {

    protected RelDataTypeFactory relDataTypeFactory;

    @Override
    public RelDataType convert(ColumnDataType columnDataType) {
        Preconditions.checkNotNull(relDataTypeFactory, "relDataTypeFactory is not set for converter %s", this.getClass().getCanonicalName());
        return convertSafely(columnDataType);
    }

    protected abstract RelDataType convertSafely(ColumnDataType columnDataType);

    @Override
    public void setTypeFactory(RelDataTypeFactory typeFactory) {
        this.relDataTypeFactory = typeFactory;
    }
}
