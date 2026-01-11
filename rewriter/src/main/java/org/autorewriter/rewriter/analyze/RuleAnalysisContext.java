package org.autorewriter.rewriter.analyze;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.tools.Planner;
import org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment;

import java.util.Collection;

/**
 * Context that contains information during analysis phase
 *
 * @author wangyanjing <wangyanjing@kuaishou.com>
 * Created on 2024-07-31
 */
@Getter
@AllArgsConstructor
public class RuleAnalysisContext {

    private final Planner planner;

    private RelNode sourceRelNode;

    private RelNode targetRelNode;

    private Collection<ConstraintSegment> checkConstraints;

    private Collection<ConstraintSegment> transformConstraints;
}
