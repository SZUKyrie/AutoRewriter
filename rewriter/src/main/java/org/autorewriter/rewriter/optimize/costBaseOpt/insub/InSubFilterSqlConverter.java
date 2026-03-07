package org.autorewriter.rewriter.optimize.costBaseOpt.insub;

import com.google.common.collect.ImmutableList;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.rel2sql.SqlImplementor;
import org.apache.calcite.rex.RexSubQuery;
import org.apache.calcite.sql.SqlDialect;

/**
 * Extended {@link RelToSqlConverter} that natively handles
 * {@link LogicalInSubFilter}, {@link HepRelVertex}, and {@link RelSubset}
 * without requiring prior conversion.
 */
public class InSubFilterSqlConverter extends RelToSqlConverter {

    public InSubFilterSqlConverter(SqlDialect dialect) {
        super(dialect);
    }

    /**
     * Convert LogicalInSubFilter to SQL by treating it as a Filter
     * with an IN-subquery condition.
     */
    public Result visit(LogicalInSubFilter inSubFilter) {
        RexSubQuery inCondition = RexSubQuery.in(
                inSubFilter.getRight(),
                ImmutableList.of(inSubFilter.getLhsRef()));

        Result leftResult = visitInput(inSubFilter, 0,
                SqlImplementor.Clause.WHERE);

        Builder builder = leftResult.builder(inSubFilter, SqlImplementor.Clause.WHERE);
        builder.setWhere(builder.context.toSql(null, inCondition));
        return builder.result();
    }

    /**
     * Unwrap HepPlanner's HepRelVertex wrapper and dispatch to the inner node.
     * HepRelVertex appears when Instantiation captures nodes from HepPlanner context.
     */
    public Result visit(HepRelVertex vertex) {
        return dispatch(vertex.getCurrentRel());
    }

    /**
     * Unwrap VolcanoPlanner's RelSubset wrapper and dispatch to the best plan.
     */
    public Result visit(RelSubset subset) {
        RelNode best = subset.getBest();
        if (best == null) {
            // Fallback to original
            for (RelNode rel : subset.getRels()) {
                if (!(rel instanceof RelSubset)) {
                    best = rel;
                    break;
                }
            }
        }
        if (best != null) {
            return dispatch(best);
        }
        throw new AssertionError("Cannot resolve empty RelSubset: " + subset);
    }
}
