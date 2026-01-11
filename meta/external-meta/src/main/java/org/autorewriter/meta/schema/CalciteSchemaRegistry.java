package org.autorewriter.meta.schema;

import org.apache.calcite.jdbc.CalciteSchema;
import org.autorewriter.common.enums.TableEngine;

import java.util.HashMap;
import java.util.Map;

public class CalciteSchemaRegistry {
    private static final Map<TableEngine, CalciteSchema> ENGINE_SCHEMA_MAP = new HashMap();

    private static void initPostgresSchema() {

    }

    public static CalciteSchema getCalciteSchema(TableEngine tableEngine) {
        CalciteSchema calciteSchema = ENGINE_SCHEMA_MAP.get(tableEngine);
        if (calciteSchema == null) {
            throw new IllegalArgumentException(String.format("invalid table engine %s for calcite schema", tableEngine.name()));
        }
        return calciteSchema;
    }
}
