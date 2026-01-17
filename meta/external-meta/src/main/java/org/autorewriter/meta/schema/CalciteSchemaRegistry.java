package org.autorewriter.meta.schema;

import org.apache.calcite.jdbc.CalciteSchema;
import org.autorewriter.common.enums.TableEngine;

import java.util.HashMap;
import java.util.Map;

public class CalciteSchemaRegistry {
    private static final Map<TableEngine, CalciteSchema> ENGINE_SCHEMA_MAP = new HashMap();

    private static void initPostgresSchema() {

    }

    /**
     * Get the CalciteSchema for a specific table engine.
     */
    public static CalciteSchema getCalciteSchema(TableEngine tableEngine) {
        CalciteSchema calciteSchema = ENGINE_SCHEMA_MAP.get(tableEngine);
        if (calciteSchema == null) {
            throw new IllegalArgumentException(String.format("invalid table engine %s for calcite schema", tableEngine.name()));
        }
        return calciteSchema;
    }

    /**
     * Get the virtual schema for rule validation.
     * This schema contains virtual tables (RULE_T0...RULE_T9) with virtual columns (RULE_A0...RULE_A19).
     * It is used during rule template validation to provide a mock schema.
     */
    public static CalciteSchema getRuleValidationSchema() {
        // Import the schema builder dynamically to avoid circular dependencies
        try {
            Class<?> builderClass = Class.forName("org.autorewriter.rewriter.schema.RuleSchemaBuilder");
            java.lang.reflect.Method method = builderClass.getMethod("buildRuleSchema");
            return (CalciteSchema) method.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create rule validation schema", e);
        }
    }
}
