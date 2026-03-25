package org.autorewriter.meta.schema;

import com.google.common.collect.ImmutableList;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.autorewriter.common.enums.TableEngine;
import org.autorewriter.meta.schema.postgres.PostgresConnectionConfig;
import org.autorewriter.meta.schema.postgres.PostgresConnectionManager;
import org.autorewriter.meta.schema.postgres.PostgresSchemaService;
import org.autorewriter.meta.schema.postgres.PostgresJdbcSchemaService;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import static org.autorewriter.common.constant.NotationConstants.EMPTY_STRING;

public class CalciteSchemaRegistry {
    private static final Map<TableEngine, CalciteSchema> ENGINE_SCHEMA_MAP = new HashMap<>();

    /**
     * Initialize PostgreSQL schema with JDBC connection
     *
     * @param configName configuration name for this PostgreSQL instance
     * @param config PostgreSQL connection configuration
     */
    public static void initPostgresSchema(String configName, PostgresConnectionConfig config) {
        PostgresConnectionManager.registerConfig(configName, config);

        PostgresSchemaService schemaService = new PostgresSchemaService(configName);
        ProxySchema proxySchema = new ProxySchema(ImmutableList.of(), schemaService);

        CalciteSchema rootSchema = CalciteSchema.createRootSchema(false, false, "", proxySchema);
        ENGINE_SCHEMA_MAP.put(TableEngine.POSTGRESQL, rootSchema);
    }

    /**
     * Initialize PostgreSQL schema with JDBC DataSource
     * 参考 adaptiveengine 的 H2 初始化方式
     *
     * @param dataSource JDBC DataSource
     * @param typeFactory RelDataTypeFactory for type conversions
     */
    public static void initPostgresSchemaWithDataSource(DataSource dataSource, RelDataTypeFactory typeFactory) {
        PostgresJdbcSchemaService schemaService = new PostgresJdbcSchemaService(dataSource, typeFactory);
        ProxySchema proxySchema = new ProxySchema(ImmutableList.of(), schemaService);

        KwaiCalciteSchema kwaiCalciteSchema = new KwaiCalciteSchema(null, proxySchema, EMPTY_STRING);
        ENGINE_SCHEMA_MAP.put(TableEngine.POSTGRESQL, kwaiCalciteSchema);
    }

    /**
     * Initialize PostgreSQL schema with JDBC URL (convenience method)
     *
     * @param url JDBC URL (e.g., "jdbc:postgresql://localhost:55555/mydb")
     * @param username database username
     * @param password database password
     */
    public static void initPostgresSchemaWithJdbcUrl(String url, String username, String password) {
        String driverClassName = "org.postgresql.Driver";
        DataSource dataSource = JdbcSchema.dataSource(url, driverClassName, username, password);
        RelDataTypeFactory typeFactory = new SqlTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
        initPostgresSchemaWithDataSource(dataSource, typeFactory);
    }

    /**
     * Initialize PostgreSQL schema with default configuration
     */
    private static void initPostgresSchema() {
        // This method can be used for default initialization if needed
        // Currently left empty as initialization should be done explicitly
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
