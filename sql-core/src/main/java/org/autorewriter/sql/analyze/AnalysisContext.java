package org.autorewriter.sql.analyze;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.tools.Planner;

/**
 * Context that contains information during analysis phase
 *
 * @author wangyanjing <wangyanjing@kuaishou.com>
 * Created on 2024-07-31
 */
@Getter
@AllArgsConstructor
public class AnalysisContext {

    // planner from Frameworks#getPlanner
    private final Planner planner;

    // relNode that converted from sqlNode
    private RelNode relNode;
}
