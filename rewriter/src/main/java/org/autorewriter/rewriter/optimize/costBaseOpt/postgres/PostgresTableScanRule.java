package org.autorewriter.rewriter.optimize.costBaseOpt.postgres;

import org.apache.calcite.adapter.jdbc.JdbcConvention;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.convert.ConverterRule;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Rule that converts a {@link TableScan} from {@link Convention#NONE}
 * to {@link JdbcConvention}, producing a {@link PostgresTableScan}.
 */
public class PostgresTableScanRule extends ConverterRule {

    public static PostgresTableScanRule create(JdbcConvention out) {
        return Config.INSTANCE
                .withConversion(TableScan.class, Convention.NONE, out,
                        "PostgresTableScanRule")
                .withRuleFactory(PostgresTableScanRule::new)
                .toRule(PostgresTableScanRule.class);
    }

    protected PostgresTableScanRule(Config config) {
        super(config);
    }

    @Override
    public @Nullable RelNode convert(RelNode rel) {
        TableScan scan = (TableScan) rel;
        return new PostgresTableScan(
                rel.getCluster(),
                scan.getTable(),
                (JdbcConvention) getOutConvention());
    }
}
