package org.autorewriter.rewriter.optimize.costBaseOpt;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalAggregate;
import org.autorewriter.rewriter.analyze.RuleAnalysisContext;

/**
 * Utility for detecting and stripping DISTINCT aggregates (LogicalAggregate with empty aggCallList)
 * from rule templates.
 *
 * <p>WeTune-style rule templates always produce a LogicalAggregate for DISTINCT, but Calcite query
 * plans may skip it when primary-key uniqueness is detected. This utility strips the Aggregate
 * from rule templates to create "downgraded" versions that can match such plans.
 */
public final class DistinctAggregateStripper {

    private DistinctAggregateStripper() {
        // utility class -- not instantiable
    }

    /**
     * Returns true if the given node is a {@link LogicalAggregate} with an empty aggCallList,
     * i.e., it represents a DISTINCT operation (no actual aggregate functions).
     *
     * @param node the RelNode to check
     * @return true if node is a DISTINCT aggregate
     */
    public static boolean isDistinctAggregate(RelNode node) {
        if (!(node instanceof LogicalAggregate)) {
            return false;
        }
        LogicalAggregate agg = (LogicalAggregate) node;
        return agg.getAggCallList().isEmpty();
    }

    /**
     * If the root is a DISTINCT aggregate (LogicalAggregate with empty aggCallList),
     * returns its sole child. Otherwise returns null.
     *
     * @param node the RelNode to strip
     * @return the child of the DISTINCT aggregate, or null if not a DISTINCT aggregate
     */
    public static RelNode strip(RelNode node) {
        if (!isDistinctAggregate(node)) {
            return null;
        }
        return node.getInput(0);
    }

    /**
     * Strips the DISTINCT aggregate from the source template root, and optionally from the
     * target template root if it is also a DISTINCT aggregate. Returns a new
     * {@link RuleAnalysisContext} with the stripped templates but the same constraints.
     *
     * <p><b>Precondition:</b> the caller must verify that
     * {@code isDistinctAggregate(ctx.getSourceRelNode())} is true before calling this method.
     *
     * @param ctx the original rule analysis context
     * @return a new RuleAnalysisContext with DISTINCT aggregates stripped from source (and
     *         optionally target)
     * @throws IllegalArgumentException if the source root is not a DISTINCT aggregate
     */
    public static RuleAnalysisContext stripBoth(RuleAnalysisContext ctx) {
        RelNode strippedSource = strip(ctx.getSourceRelNode());
        if (strippedSource == null) {
            throw new IllegalArgumentException(
                    "Source root is not a DISTINCT aggregate: " + ctx.getSourceRelNode());
        }

        RelNode targetRoot = ctx.getTargetRelNode();
        RelNode strippedTarget = strip(targetRoot);
        // If target is also a DISTINCT aggregate, use the stripped version; otherwise keep as-is
        RelNode finalTarget = strippedTarget != null ? strippedTarget : targetRoot;

        return new RuleAnalysisContext(
                strippedSource,
                finalTarget,
                ctx.getMatchConstraints(),
                ctx.getRewriteConstraints());
    }
}
