package org.autorewriter.common.enums;

/**
 * The table engine from which the table metadata is retrieved,
 * metadata includes table name, columns, etc.
 */
public enum TableEngine {
    HIVE,
    CLICKHOUSE,
    POSTGRESQL,
    MYSQL,
    REWRITE_RULE;

    public ComputeEngine getDefaultComputeEngine() {
        switch (this) {
            case CLICKHOUSE:
                return ComputeEngine.CLICKHOUSE;
            case POSTGRESQL:
                return ComputeEngine.POSTGRESQL;
            case REWRITE_RULE:
                return ComputeEngine.REWRITE_RULE;
            case MYSQL:
                return ComputeEngine.MYSQL;
            default:
                throw new IllegalArgumentException("Unknown table engine: " + this);
        }
    }
}
