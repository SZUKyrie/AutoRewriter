package org.autorewriter.sql.common;

import com.google.common.base.Preconditions;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.autorewriter.common.enums.TableEngine;

import java.util.HashMap;
import java.util.Map;

public class RelDataTypeSystemRegistry {
    private static final Map<TableEngine, RelDataTypeSystem> REGISTRY = new HashMap<>();

    static {
        REGISTRY.put(TableEngine.CLICKHOUSE, RelDataTypeSystem.DEFAULT);
        REGISTRY.put(TableEngine.POSTGRESQL, RelDataTypeSystem.DEFAULT);
    }

    public static RelDataTypeSystem getRelDataTypeSystem(TableEngine tableEngine) {
        RelDataTypeSystem relDataTypeSystem = REGISTRY.get(tableEngine);
        Preconditions.checkNotNull(relDataTypeSystem, String.format("no rel data type system for table engine %s", tableEngine));
        return relDataTypeSystem;
    }
}
