package org.autorewriter.common.enums;

import lombok.Getter;

/**
 * @author fengzanfeng <fengzanfeng@kuaishou.com>
 * Created on 2024-07-02
 */
@Getter
public enum ComputeEngine {
    SPARK,
    CLICKHOUSE,
    POSTGRESQL,
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
                return TableEngine.HIVE;
            case CLICKHOUSE:
                return TableEngine.CLICKHOUSE;
            case POSTGRESQL:
                return TableEngine.POSTGRESQL;
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
            default:
                throw new IllegalArgumentException("unsupported table engine: " + this);
        }
    }

    public boolean useMultiDialectParser() {
        switch (this) {
            case POSTGRESQL:
            case CLICKHOUSE:
            case SPARK:
                return true;
            default:
                throw new IllegalArgumentException("No parser for computeEngine: [" + this + "] found");
        }
    }
}
