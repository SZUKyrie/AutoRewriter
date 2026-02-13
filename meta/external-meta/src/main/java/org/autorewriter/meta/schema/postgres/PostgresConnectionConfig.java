package org.autorewriter.meta.schema.postgres;

import lombok.Builder;
import lombok.Data;

/**
 * PostgreSQL connection configuration
 */
@Data
@Builder
public class PostgresConnectionConfig {
    private String host;
    private int port;
    private String database;
    private String schema;
    private String username;
    private String password;

    @Builder.Default
    private int maxPoolSize = 10;

    @Builder.Default
    private int minIdle = 2;

    @Builder.Default
    private long connectionTimeout = 30000L;

    public String getJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
    }
}

