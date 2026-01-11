package org.autorewriter.sql.common;

import org.autorewriter.common.enums.ComputeEngine;

import static org.autorewriter.sql.analyze.AnalysisConfigs.CLICKHOUSE_CALCITE_CONNECTION_CONFIG;
import static org.autorewriter.sql.analyze.AnalysisConfigs.COMMON_CALCITE_CONNECTION_CONFIG;

/**
 * Create planner context for different table engine
 *
 * @author wangyanjing <wangyanjing@kuaishou.com>
 * Created on 2024-08-01
 */
public class PlannerContextFactory {

    public static PlannerContext create(ComputeEngine computeEngine) {
        switch (computeEngine) {
            case CLICKHOUSE:
                return new PlannerContext(CLICKHOUSE_CALCITE_CONNECTION_CONFIG);
            default:
                return new PlannerContext(COMMON_CALCITE_CONNECTION_CONFIG);
        }
    }
}
