package org.autorewriter.rewriter.optimize.costBaseOpt;

import org.apache.calcite.adapter.jdbc.JdbcConvention;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;
import org.autorewriter.rewriter.optimize.costBaseOpt.postgres.PostgresTableScan;
import org.autorewriter.rewriter.optimize.trace.OptimizationTrace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CostBaseOptimizer with PostgreSQL physical operators.
 */
public class CostBaseOptimizerTest {

    private RelBuilder relBuilder;

    @BeforeEach
    public void setup() {
        SchemaPlus rootSchema = Frameworks.createRootSchema(true);

        Table customersTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return typeFactory.builder()
                        .add("id", SqlTypeName.INTEGER)
                        .add("name", SqlTypeName.VARCHAR, 100)
                        .add("email", SqlTypeName.VARCHAR, 200)
                        .build();
            }
        };
        rootSchema.add("customers", customersTable);

        Table ordersTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return typeFactory.builder()
                        .add("order_id", SqlTypeName.INTEGER)
                        .add("customer_id", SqlTypeName.INTEGER)
                        .add("amount", SqlTypeName.DECIMAL, 10, 2)
                        .add("order_date", SqlTypeName.DATE)
                        .build();
            }
        };
        rootSchema.add("orders", ordersTable);

        FrameworkConfig config = Frameworks.newConfigBuilder()
                .defaultSchema(rootSchema)
                .build();
        relBuilder = RelBuilder.create(config);
    }

    @Test
    public void testSimpleScanProducesPhysicalPlan() {
        RelNode query = relBuilder.scan("customers").build();

        CostBaseOptimizer optimizer = new CostBaseOptimizer();
        RelNode result = optimizer.optimize(query);

        assertNotNull(result);
        assertTrue(result instanceof PostgresTableScan,
                "Expected PostgresTableScan but got " + result.getClass().getSimpleName());
        assertTrue(result.getConvention() instanceof JdbcConvention,
                "Expected JdbcConvention but got " + result.getConvention());
    }

    @Test
    public void testProjectFilterProducesPhysicalPlan() {
        RelNode query = relBuilder
                .scan("customers")
                .filter(relBuilder.call(SqlStdOperatorTable.GREATER_THAN,
                        relBuilder.field("id"), relBuilder.literal(100)))
                .project(relBuilder.field("name"))
                .build();

        CostBaseOptimizer optimizer = new CostBaseOptimizer();
        RelNode result = optimizer.optimize(query);

        assertNotNull(result);
        assertTrue(result.getConvention() instanceof JdbcConvention,
                "Root should be in JdbcConvention");
        assertAllNodesInJdbcConvention(result);
    }

    @Test
    public void testJoinProducesPhysicalPlan() {
        relBuilder.clear();
        RelNode left = relBuilder.scan("customers").build();
        relBuilder.clear();
        RelNode right = relBuilder.scan("orders").build();
        relBuilder.clear();
        RelNode query = relBuilder
                .push(left)
                .push(right)
                .join(JoinRelType.INNER,
                        relBuilder.equals(
                                relBuilder.field(2, 0, "id"),
                                relBuilder.field(2, 1, "customer_id")))
                .build();

        CostBaseOptimizer optimizer = new CostBaseOptimizer();
        RelNode result = optimizer.optimize(query);

        assertNotNull(result);
        assertAllNodesInJdbcConvention(result);
    }

    @Test
    public void testAggregateProducesPhysicalPlan() {
        RelNode query = relBuilder
                .scan("orders")
                .aggregate(relBuilder.groupKey("customer_id"),
                        relBuilder.sum(false, "total_amount",
                                relBuilder.field("amount")))
                .build();

        CostBaseOptimizer optimizer = new CostBaseOptimizer();
        RelNode result = optimizer.optimize(query);

        assertNotNull(result);
        assertAllNodesInJdbcConvention(result);
    }

    @Test
    public void testSortProducesPhysicalPlan() {
        RelNode query = relBuilder
                .scan("customers")
                .sort(relBuilder.field("name"))
                .build();

        CostBaseOptimizer optimizer = new CostBaseOptimizer();
        RelNode result = optimizer.optimize(query);

        assertNotNull(result);
        assertAllNodesInJdbcConvention(result);
    }

    @Test
    public void testWithLogicalTransformationRules() {
        RelNode query = relBuilder
                .scan("customers")
                .filter(relBuilder.call(SqlStdOperatorTable.GREATER_THAN,
                        relBuilder.field("id"), relBuilder.literal(100)))
                .project(relBuilder.field("name"), relBuilder.field("id"))
                .build();

        CostBaseOptimizer optimizer = new CostBaseOptimizer();
        optimizer.addRule(CoreRules.PROJECT_FILTER_TRANSPOSE);
        RelNode result = optimizer.optimize(query);

        assertNotNull(result);
        assertAllNodesInJdbcConvention(result);
    }

    @Test
    public void testOptimizationTraceRecordsSteps() {
        RelNode query = relBuilder
                .scan("customers")
                .filter(relBuilder.call(SqlStdOperatorTable.GREATER_THAN,
                        relBuilder.field("id"), relBuilder.literal(100)))
                .build();

        CostBaseOptimizer optimizer = new CostBaseOptimizer();
        OptimizationTrace trace = new OptimizationTrace();
        RelNode result = optimizer.optimize(query, trace);

        assertNotNull(result);
        assertTrue(trace.firedCount() > 0,
                "Expected at least one rule fire in the trace");
    }

    /**
     * Recursively asserts all nodes in the plan tree have JdbcConvention.
     */
    private void assertAllNodesInJdbcConvention(RelNode node) {
        assertTrue(node.getConvention() instanceof JdbcConvention,
                "Node " + node.getClass().getSimpleName()
                        + " has convention " + node.getConvention()
                        + " but expected JdbcConvention");
        for (RelNode input : node.getInputs()) {
            assertAllNodesInJdbcConvention(input);
        }
    }
}
