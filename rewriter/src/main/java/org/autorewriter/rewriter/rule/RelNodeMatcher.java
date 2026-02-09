package org.autorewriter.rewriter.rule;

import org.apache.calcite.rel.RelNode;
import java.util.Map;

/**
 * Interface for matching RelNode templates against query RelNodes.
 */
public interface RelNodeMatcher<T extends RelNode> {

    /**
     * Match a template RelNode against a query RelNode.
     *
     * @param template the template RelNode
     * @param query the query RelNode
     * @param bindings the bindings map to store matched placeholders
     * @return true if the match succeeds, false otherwise
     */
    boolean match(T template, T query, Map<String, Object> bindings);
}
