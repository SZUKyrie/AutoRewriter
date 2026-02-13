package org.autorewriter.meta.schema.postgres;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PostgreSQL connection manager using simple JDBC connections
 */
@Slf4j
public class PostgresConnectionManager {
    private static final Map<String, PostgresConnectionConfig> CONFIG_MAP = new ConcurrentHashMap<>();

    /**
     * Register a PostgreSQL connection configuration
     */
    public static void registerConfig(String name, PostgresConnectionConfig config) {
        CONFIG_MAP.put(name, config);
        log.info("Registered PostgreSQL config: {}", name);
    }

    /**
     * Get a connection for the given config name
     */
    public static Connection getConnection(String configName) throws SQLException {
        PostgresConnectionConfig config = CONFIG_MAP.get(configName);
        if (config == null) {
            throw new IllegalArgumentException("No PostgreSQL config found for: " + configName);
        }

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL driver not found", e);
        }

        return java.sql.DriverManager.getConnection(
            config.getJdbcUrl(),
            config.getUsername(),
            config.getPassword()
        );
    }

    /**
     * Get configuration by name
     */
    public static PostgresConnectionConfig getConfig(String configName) {
        return CONFIG_MAP.get(configName);
    }

    /**
     * Remove a configuration
     */
    public static void removeConfig(String configName) {
        CONFIG_MAP.remove(configName);
        log.info("Removed PostgreSQL config: {}", configName);
    }

    /**
     * Clear all configurations
     */
    public static void clearAll() {
        CONFIG_MAP.clear();
        log.info("Cleared all PostgreSQL configs");
    }
}

