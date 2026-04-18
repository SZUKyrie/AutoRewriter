package org.autorewriter.common.enums;

import lombok.Getter;

@Getter
public enum ComputeEngine {
    SPARK,
    CLICKHOUSE,
    POSTGRESQL,
    MYSQL,
    HIVE,
    REWRITE_RULE;

    public static ComputeEngine fromString(String name) {
        for (ComputeEngine ce : ComputeEngine.values()) {
            if (ce.name().equalsIgnoreCase(name)) {
                return ce;
            }
        }
        throw new IllegalArgumentException("No computeEngine with name: [" + name + "] found");
    }

    public TableEngine getTableEngine() {
        switch (this) {
            case SPARK:
            case HIVE:
                return TableEngine.HIVE;
            case CLICKHOUSE:
                return TableEngine.CLICKHOUSE;
            case POSTGRESQL:
                return TableEngine.POSTGRESQL;
            case MYSQL:
                return TableEngine.MYSQL;
            case REWRITE_RULE:
                return TableEngine.REWRITE_RULE;
            default:
                throw new IllegalArgumentException("No dialect for computeEngine: [" + this + "] found");
        }
    }

    public String getDialectName() {
        switch (this) {
            case CLICKHOUSE:
                return "ClickHouse";
            case SPARK:
                return "Spark";
            case POSTGRESQL:
                return "PostgreSQL";
            case HIVE:
                return "Hive";
            case MYSQL:
                return "MySQL";
            default:
                throw new IllegalArgumentException("unsupported table engine: " + this);
        }
    }

    public boolean useMultiDialectParser() {
        switch (this) {
            case POSTGRESQL:
            case CLICKHOUSE:
            case SPARK:
            case HIVE:
            case MYSQL:
                return true;
            default:
                throw new IllegalArgumentException("No parser for computeEngine: [" + this + "] found");
        }
    }
}
