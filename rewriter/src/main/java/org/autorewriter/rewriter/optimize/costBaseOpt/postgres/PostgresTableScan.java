package org.autorewriter.rewriter.optimize.costBaseOpt.postgres;

import org.apache.calcite.adapter.jdbc.JdbcConvention;
import org.apache.calcite.adapter.jdbc.JdbcImplementor;
import org.apache.calcite.adapter.jdbc.JdbcRel;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

import java.util.Collections;
import java.util.List;

/**
 * Physical table scan operator for PostgreSQL convention.
 * Bridges the project's AbstractTable-based tables to JdbcConvention,
 * allowing the VolcanoPlanner to produce physical query plans.
 */
public class PostgresTableScan extends TableScan implements JdbcRel {

    public PostgresTableScan(RelOptCluster cluster, RelOptTable table,
                             JdbcConvention convention) {
        super(cluster, cluster.traitSetOf(convention), Collections.emptyList(), table);
    }

    @Override
    public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs) {
        assert inputs.isEmpty();
        PostgresTableScan scan = new PostgresTableScan(getCluster(), table,
                (JdbcConvention) getConvention());
        return scan;
    }

    @Override
    public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
        RelOptCost cost = super.computeSelfCost(planner, mq);
        if (cost == null) {
            return null;
        }
        return cost.multiplyBy(JdbcConvention.COST_MULTIPLIER);
    }

    @Override
    public JdbcImplementor.Result implement(JdbcImplementor implementor) {
        throw new UnsupportedOperationException(
                "PostgresTableScan does not support SQL generation via JdbcImplementor");
    }
}
