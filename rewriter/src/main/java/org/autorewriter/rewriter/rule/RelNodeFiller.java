package org.autorewriter.rewriter.rule;

import org.apache.calcite.rel.RelNode;
import java.util.Map;

/**
 * Interface for filling target template RelNodes with bound values.
 */
public interface RelNodeFiller<T extends RelNode> {

    /**
     * Fill a target template RelNode with bound values.
     *
     * @param template the target template RelNode
     * @param bindings the bindings map containing placeholder values
     * @return the filled RelNode
     */
    RelNode fill(T template, Map<String, Object> bindings);
}
