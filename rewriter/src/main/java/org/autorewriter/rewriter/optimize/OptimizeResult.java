package org.autorewriter.rewriter.optimize;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.autorewriter.rewriter.optimize.trace.OptimizationTrace;
import org.autorewriter.rewriter.optimize.trace.RuleApplicationStep;

import java.util.Collections;
import java.util.List;

@Slf4j
@Getter
@Setter
public class OptimizeResult {
    String sql;
    String queryId;
    RelNode originalRelNode;
    RelNode optimizedRelNode;
    double optimizationTimeInMs;
    /*whether the original sql is rewritten by any rule, which is determined by whether the optimizedRelNode is different from the originalRelNode*/
    boolean isRewritten;

    /** Full optimization trace; null if tracing was disabled */
    OptimizationTrace trace;

    public OptimizeResult(String sql, String querId) {
        this.sql = sql;
        this.queryId = querId;
    }

    /** Every rule that fired, in order. Empty list if tracing was disabled. */
    public List<RuleApplicationStep> getRuleApplicationSteps() {
        return trace == null ? Collections.emptyList() : trace.getSteps();
    }

    /**
     * The produced RelNode after each successful rule fire, in order.
     * The last element is the final optimized tree.
     * Empty list if tracing was disabled.
     */
    public List<RelNode> getIntermediateRelNodes() {
        return trace == null ? Collections.emptyList() : trace.getIntermediateRelNodes();
    }
}
