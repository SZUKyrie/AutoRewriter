package org.autorewriter.rewriter.optimize.costBaseOpt.postgres;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttleImpl;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.rex.RexSubQuery;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.sql.SqlKind;

import java.util.Collections;
import java.util.List;

/**
 * Preprocessor that splits LogicalFilter nodes with AND-conjoined conditions
 * into a chain of individual LogicalFilter nodes, one per conjunct.
 * Also descends into RexSubQuery to process subquery RelNode trees.
 * <p>
 * Before: Filter(AND(p0, p1, p2), Input)
 * After:  Filter(p0, Filter(p1, Filter(p2, Input)))
 * <p>
 * This enables rule templates like Filter(Filter(Input)) to match
 * against plans where Calcite merged multiple predicates into one Filter.
 * <p>
 * Usage: call {@code FilterSplitter.split(relNode)} before CBO optimization.
 */
public class FilterSplitter extends RelShuttleImpl {

    /**
     * RexShuttle that descends into RexSubQuery and applies FilterSplitter
     * to the subquery's RelNode tree.
     */
    private final RexShuttle rexShuttle = new RexShuttle() {
        @Override
        public RexNode visitSubQuery(RexSubQuery subQuery) {
            RelNode newRel = subQuery.rel.accept(FilterSplitter.this);
            if (newRel != subQuery.rel) {
                return subQuery.clone(newRel);
            }
            return subQuery;
        }
    };

    public static RelNode split(RelNode root) {
        return root.accept(new FilterSplitter());
    }

    @Override
    public RelNode visit(LogicalFilter filter) {
        // First, recursively process the direct child
        RelNode newInput = filter.getInput().accept(this);

        // Process RexSubQuery nodes inside the condition
        RexNode condition = filter.getCondition().accept(rexShuttle);

        if (condition.getKind() != SqlKind.AND) {
            if (newInput == filter.getInput() && condition == filter.getCondition()) {
                return filter;
            }
            return filter.copy(filter.getTraitSet(), newInput, condition);
        }

        List<RexNode> conjuncts = RexUtil.flattenAnd(
                Collections.singletonList(condition));

        if (conjuncts.size() <= 1) {
            if (newInput == filter.getInput() && condition == filter.getCondition()) {
                return filter;
            }
            return filter.copy(filter.getTraitSet(), newInput, condition);
        }

        // Build chain bottom-up: Filter(p0, Filter(p1, ... Filter(pN, input)))
        RelNode current = newInput;
        for (int i = conjuncts.size() - 1; i >= 0; i--) {
            current = LogicalFilter.create(current, conjuncts.get(i));
        }

        return current;
    }
}
