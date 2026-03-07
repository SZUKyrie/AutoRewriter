package org.autorewriter.rewriter.optimize.costBaseOpt.postgres;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttleImpl;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.rex.RexSubQuery;
import org.apache.calcite.rex.RexUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-processor that merges consecutive Filter nodes into a single Filter
 * with AND-conjoined predicates. This is the inverse of {@link FilterSplitter}.
 * <p>
 * Before: Filter(p0, Filter(p1, Filter(p2, Input)))
 * After:  Filter(AND(p0, p1, p2), Input)
 * <p>
 * This prevents Calcite's RelToSqlConverter from generating nested subqueries
 * for each individual filter predicate.
 * <p>
 * Also descends into RexSubQuery to merge filters in subquery RelNode trees.
 * <p>
 * Usage: call {@code FilterMerger.merge(relNode)} after CBO optimization
 * and before SQL generation.
 */
public class FilterMerger extends RelShuttleImpl {

    private final RexShuttle rexShuttle = new RexShuttle() {
        @Override
        public RexNode visitSubQuery(RexSubQuery subQuery) {
            RelNode newRel = subQuery.rel.accept(FilterMerger.this);
            if (newRel != subQuery.rel) {
                return subQuery.clone(newRel);
            }
            return subQuery;
        }
    };

    public static RelNode merge(RelNode root) {
        return root.accept(new FilterMerger());
    }

    @Override
    public RelNode visit(LogicalFilter filter) {
        return mergeFilterChain(filter);
    }

    @Override
    public RelNode visit(RelNode other) {
        // Handle non-LogicalFilter Filter subclasses (e.g., JdbcFilter)
        if (other instanceof Filter) {
            return mergeFilterChain((Filter) other);
        }
        return super.visit(other);
    }

    private RelNode mergeFilterChain(Filter topFilter) {
        // Collect all consecutive filter predicates
        List<RexNode> predicates = new ArrayList<>();
        RelNode current = topFilter;

        while (current instanceof Filter) {
            Filter f = (Filter) current;
            // Process RexSubQuery inside the condition
            RexNode condition = f.getCondition().accept(rexShuttle);
            predicates.add(condition);
            current = f.getInput();
        }

        if (predicates.size() <= 1) {
            // Only one filter — just process child and subqueries
            RelNode newInput = current.accept(this);
            RexNode condition = topFilter.getCondition().accept(rexShuttle);
            if (newInput == topFilter.getInput() && condition == topFilter.getCondition()) {
                return topFilter;
            }
            return topFilter.copy(topFilter.getTraitSet(), newInput, condition);
        }

        // Multiple consecutive filters — merge into single AND predicate
        RelNode newInput = current.accept(this);
        RexNode merged = RexUtil.composeConjunction(
                topFilter.getCluster().getRexBuilder(), predicates);
        return topFilter.copy(topFilter.getTraitSet(), newInput, merged);
    }
}
