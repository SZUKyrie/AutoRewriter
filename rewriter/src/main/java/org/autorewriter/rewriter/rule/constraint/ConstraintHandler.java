package org.autorewriter.rewriter.rule.constraint;

import java.util.Map;

/**
 * Interface for evaluating a specific type of match constraint.
 */
public interface ConstraintHandler {

    /**
     * @return the constraint type name this handler supports (e.g. "UNIQUE", "NOT_NULL")
     */
    String getType();

    /**
     * Evaluate whether the constraint is satisfied.
     *
     * @param params   constraint parameters (e.g. ["t0", "a0"])
     * @param bindings current placeholder bindings from rule matching
     * @return true if the constraint is satisfied
     */
    boolean evaluate(String[] params, Map<String, Object> bindings);
}

