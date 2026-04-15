package org.autorewriter.rewriter.optimize.trace;

/**
 * Callback interface for consuming an {@link OptimizationTrace} after each query optimization.
 *
 * <p>Implementations (e.g. GraphModule in the {@code graph} module) live in dependent modules,
 * so this interface is defined here in {@code rewriter} to avoid circular dependencies.
 */
public interface TraceConsumer {
    /**
     * Called once per query optimization with the completed trace.
     *
     * @param trace the optimization trace produced for the query
     */
    void consume(OptimizationTrace trace);
}
