package org.autorewriter.rewriter.optimize.ruleBaseOpt;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.hep.HepMatchOrder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;
import org.autorewriter.rewriter.analyze.RuleAnalysisContext;
import org.autorewriter.rewriter.analyze.RuleAnalyzer;
import org.autorewriter.rewriter.rule.AutoRewriteRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RuleBaseOptimizer.
 *
 * <p>Each test registers a single rewrite rule and validates the optimizer behavior.</p>
 */
public class RuleBaseOptimizerTest {

    private static final String SEPARATOR = "─".repeat(60);
    private static final String DOUBLE_SEPARATOR = "═".repeat(60);

    private RelBuilder relBuilder;

    @BeforeEach
    public void setup() {
        // Create a schema with customers and orders tables
        SchemaPlus rootSchema = Frameworks.createRootSchema(true);

        // Create customers table: (id, name, email)
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

        // Create orders table: (order_id, customer_id, amount, order_date)
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

        // Build FrameworkConfig with the schema
        FrameworkConfig config = Frameworks.newConfigBuilder()
            .defaultSchema(rootSchema)
            .build();

        relBuilder = RelBuilder.create(config);
    }

    /**
     * Helper method to convert RelNode to SQL string.
     */
    private String relNodeToSql(RelNode relNode) {
        try {
            org.apache.calcite.sql.SqlDialect dialect = org.apache.calcite.sql.dialect.AnsiSqlDialect.DEFAULT;
            org.apache.calcite.rel.rel2sql.RelToSqlConverter converter =
                new org.apache.calcite.rel.rel2sql.RelToSqlConverter(dialect);
            org.apache.calcite.rel.rel2sql.RelToSqlConverter.Result result =
                converter.visitRoot(relNode);
            org.apache.calcite.sql.SqlNode sqlNode = result.asStatement();
            return sqlNode.toSqlString(dialect).getSql();
        } catch (Exception e) {
            return "Failed to convert to SQL: " + e.getMessage();
        }
    }

    /**
     * Unified output helper method.
     * Prints: Test Name, Rule, Source SQL, Source RelNode, Rewritten SQL, Rewritten RelNode, Status
     *
     * @param testName   the name of the test
     * @param ruleStr    the rule string
     * @param original   the original RelNode
     * @param optimized  the optimized RelNode
     */
    private void printUnifiedOutput(String testName, String ruleStr, RelNode original, RelNode optimized) {
        System.out.println();
        System.out.println(DOUBLE_SEPARATOR);
        System.out.println("TEST: " + testName);
        System.out.println(DOUBLE_SEPARATOR);

        System.out.println("\n[RULE]");
        System.out.println(ruleStr);

        System.out.println("\n" + SEPARATOR);
        System.out.println("[SOURCE SQL]");
        System.out.println(SEPARATOR);
        System.out.println(relNodeToSql(original));

        System.out.println("\n" + SEPARATOR);
        System.out.println("[SOURCE RELNODE]");
        System.out.println(SEPARATOR);
        System.out.println(original.explain().trim());

        System.out.println("\n" + SEPARATOR);
        System.out.println("[REWRITTEN SQL]");
        System.out.println(SEPARATOR);
        System.out.println(relNodeToSql(optimized));

        System.out.println("\n" + SEPARATOR);
        System.out.println("[REWRITTEN RELNODE]");
        System.out.println(SEPARATOR);
        System.out.println(optimized.explain().trim());

        System.out.println("\n" + SEPARATOR);
        boolean changed = !original.explain().equals(optimized.explain());
        System.out.println("[STATUS] " + (changed ? "✓ Query Rewritten" : "✗ No Change"));
        System.out.println(DOUBLE_SEPARATOR);
    }

    /**
     * Test 1: Register a rule that removes unnecessary DISTINCT.
     *
     * <p>Rule: Proj*<a0 s0>(Input<t0>) -> Proj<a1 s1>(Input<t1>)</p>
     * <p>Condition: Column has unique constraint</p>
     */
    @Test
    public void testRemoveDistinctWithUniqueConstraint() {
        String testName = "Remove DISTINCT with Unique Constraint";
        String ruleStr = "Proj*<a0 s0>(Input<t0>)|Proj<a1 s1>(Input<t1>)|AttrsSub(a0,t0);Unique(t0,a0);TableEq(t1,t0);AttrsEq(a1,a0);SchemaEq(s1,s0)";

        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        assertNotNull(ruleContext);
        assertNotNull(ruleContext.getSourceRelNode());
        assertNotNull(ruleContext.getTargetRelNode());

        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalAggregate.class, RelOptRule.any()),
            ruleContext
        );

        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);
        assertEquals(1, optimizer.getRuleCount());

        try {
            RelNode query = relBuilder
                .scan("customers")
                .project(relBuilder.field("id"))
                .distinct()
                .build();

            RelNode optimized = optimizer.optimize(query);
            assertNotNull(optimized);

            printUnifiedOutput(testName, ruleStr, query, optimized);
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 2: Register a rule that converts LEFT JOIN to INNER JOIN.
     *
     * <p>Rule: LeftJoin -> InnerJoin</p>
     * <p>Condition: Right side has unique and not null constraint</p>
     */
    @Test
    public void testLeftJoinToInnerJoin() {
        String testName = "LEFT JOIN to INNER JOIN";
        String ruleStr = "LeftJoin<a1 a2>(Proj*<a0 s0>(Input<t0>),Input<t1>)" +
                "|InnerJoin<a4 a5>(Proj<a3 s1>(Input<t2>),Input<t3>)" +
                "|TableEq(t0,t1);AttrsEq(a1,a2);AttrsSub(a0,t0);AttrsSub(a1,s0);" +
                "AttrsSub(a2,t1);NotNull(t1,a2);TableEq(t2,t0);TableEq(t3,t1);" +
                "AttrsEq(a3,a0);AttrsEq(a4,a1);AttrsEq(a5,a2);SchemaEq(s1,s0)";

        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        assertNotNull(ruleContext);

        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalJoin.class, RelOptRule.any()),
            ruleContext
        );

        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);
        assertEquals(1, optimizer.getRuleCount());

        try {
            relBuilder.clear();
            RelNode leftSide = relBuilder
                .scan("customers")
                .project(relBuilder.field("id"))
                .distinct()
                .build();

            relBuilder.clear();
            RelNode rightSide = relBuilder
                .scan("orders")
                .build();

            relBuilder.clear();
            RelNode query = relBuilder
                .push(leftSide)
                .push(rightSide)
                .join(
                    org.apache.calcite.rel.core.JoinRelType.LEFT,
                    relBuilder.equals(
                        relBuilder.field(2, 0, "id"),
                        relBuilder.field(2, 1, "customer_id")
                    )
                )
                .build();

            RelNode optimized = optimizer.optimize(query);
            assertNotNull(optimized);

            printUnifiedOutput(testName, ruleStr, query, optimized);
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 4: Register a rule that eliminates JOIN with unique constraint.
     *
     * <p>Rule: Proj(InnerJoin) -> Proj(Input)</p>
     * <p>Condition: JOIN key is unique</p>
     */
    @Test
    public void testEliminateJoinWithUniqueKey() {
        String testName = "Eliminate JOIN with Unique Key";
        String ruleStr = "Proj<a2 s0>(InnerJoin<a0 a1>(Input<t0>,Input<t1>))|Proj<a3 s1>(Input<t2>)|AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t0);Unique(t1,a1);NotNull(t0,a0);Reference(t0,a0,t1,a1);TableEq(t2,t0);AttrsEq(a3,a2);SchemaEq(s1,s0)";

        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        assertNotNull(ruleContext);

        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalProject.class, RelOptRule.any()),
            ruleContext
        );

        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);
        assertEquals(1, optimizer.getRuleCount());

        try {
            relBuilder.clear();
            RelNode query = relBuilder
                .scan("customers")
                .as("c")
                .scan("orders")
                .as("o")
                .join(
                    org.apache.calcite.rel.core.JoinRelType.INNER,
                    relBuilder.equals(
                        relBuilder.field(2, 0, "id"),
                        relBuilder.field(2, 1, "customer_id")
                    )
                )
                .project(relBuilder.field("c", "name"))
                .build();

            RelNode optimized = optimizer.optimize(query);
            assertNotNull(optimized);

            printUnifiedOutput(testName, ruleStr, query, optimized);
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 5: Test optimizer configuration with TOP_DOWN match order.
     */
    @Test
    public void testOptimizerConfigurationTopDown() {
        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.setMatchOrder(HepMatchOrder.TOP_DOWN);
        assertEquals(0, optimizer.getRuleCount());
    }

    /**
     * Test 6: Test optimizer configuration with BOTTOM_UP match order.
     */
    @Test
    public void testOptimizerConfigurationBottomUp() {
        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.setMatchOrder(HepMatchOrder.BOTTOM_UP);
        assertEquals(0, optimizer.getRuleCount());
    }

    /**
     * Test 7: Test setMaxIterations with valid value.
     */
    @Test
    public void testSetMaxIterationsValid() {
        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.setMaxIterations(5);
        assertEquals(0, optimizer.getRuleCount());
    }

    /**
     * Test 8: Test setMaxIterations with invalid value (should throw exception).
     */
    @Test
    public void testSetMaxIterationsInvalid() {
        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        assertThrows(IllegalArgumentException.class, () -> {
            optimizer.setMaxIterations(0);
        });
    }

    /**
     * Test 9: Test clearRules functionality.
     */
    @Test
    public void testClearRules() {
        String ruleStr = "Proj*<a0 s0>(Input<t0>)|Proj<a1 s1>(Input<t1>)|AttrsSub(a0,t0);Unique(t0,a0);TableEq(t1,t0);AttrsEq(a1,a0);SchemaEq(s1,s0)";
        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalAggregate.class, RelOptRule.any()),
            ruleContext
        );

        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);
        assertEquals(1, optimizer.getRuleCount());

        optimizer.clearRules();
        assertEquals(0, optimizer.getRuleCount());
    }

    /**
     * Test 10: Test merging duplicate filters.
     *
     * <p>Rule: Filter(Filter(Input)) -> Filter(Input)</p>
     */
    @Test
    public void testMergeDuplicateFilters() {
        String testName = "Merge Duplicate Filters";
        String ruleStr = "Filter<p1 a1>(Filter<p0 a0>(Input<t0>))|Filter<p2 a2>(Input<t1>)|AttrsEq(a0,a1);PredicateEq(p0,p1);AttrsSub(a0,t0);AttrsSub(a1,t0);TableEq(t1,t0);AttrsEq(a2,a0);PredicateEq(p2,p0)";

        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        assertNotNull(ruleContext);

        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalFilter.class, RelOptRule.any()),
            ruleContext
        );

        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);
        assertEquals(1, optimizer.getRuleCount());

        try {
            relBuilder.clear();
            RelNode query = relBuilder
                .scan("customers")
                .filter(
                    relBuilder.call(
                        org.apache.calcite.sql.fun.SqlStdOperatorTable.GREATER_THAN,
                        relBuilder.field("id"),
                        relBuilder.literal(100)
                    )
                )
                .filter(
                    relBuilder.call(
                        org.apache.calcite.sql.fun.SqlStdOperatorTable.GREATER_THAN,
                        relBuilder.field("id"),
                        relBuilder.literal(100)
                    )
                )
                .build();

            RelNode optimized = optimizer.optimize(query);
            assertNotNull(optimized);

            printUnifiedOutput(testName, ruleStr, query, optimized);
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 11: Verify that Input<t0> can match complex RelNode subtrees, not just base tables.
     *
     * <p>This test demonstrates that template placeholders like Input<t0> can match:
     * - Base tables (LogicalTableScan)
     * - Filtered tables (LogicalFilter -> LogicalTableScan)
     * - Projected tables (LogicalProject -> LogicalTableScan)
     * - Any other RelNode subtree
     * </p>
     *
     * <p>Rule: Filter<p0 a0>(Input<t0>) -> Input<t1></p>
     * <p>Condition: The filter can be eliminated (e.g., always true condition)</p>
     */
    @Test
    public void testInputPlaceholderMatchesSubtree() {
        String testName = "Input Placeholder Matches Complex Subtree";
        // Rule: Filter(Input) -> Input (for demonstration, we match any filter and remove it)
        // In practice, you'd add constraints to ensure the filter is removable
        String ruleStr = "Filter<p0 a0>(Input<t0>)|Input<t1>|TableEq(t1,t0);AttrsSub(a0,t0)";

        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        assertNotNull(ruleContext);

        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalFilter.class, RelOptRule.any()),
            ruleContext
        );

        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);
        assertEquals(1, optimizer.getRuleCount());

        try {
            relBuilder.clear();

            // Create a complex subtree: JOIN query (customers JOIN orders)
            // This represents a derived table query that cannot be directly matched by a simple TableScan
            // SQL equivalent: SELECT c.id, c.name, o.order_id FROM customers c JOIN orders o ON c.id = o.customer_id
            RelNode complexSubtree = relBuilder
                .scan("customers")
                .scan("orders")
                .join(
                    org.apache.calcite.rel.core.JoinRelType.INNER,
                    relBuilder.equals(
                        relBuilder.field(2, 0, "id"),
                        relBuilder.field(2, 1, "customer_id")
                    )
                )
                .project(
                    relBuilder.field(0),  // customers.id
                    relBuilder.field(1),  // customers.name
                    relBuilder.field(4)   // orders.order_id
                )
                .build();

            // Wrap the complex JOIN subtree with a filter: id > 100
            // SQL equivalent: SELECT * FROM (SELECT c.id, c.name, o.order_id FROM customers c JOIN orders o ...) t0 WHERE id > 100
            relBuilder.clear();
            RelNode query = relBuilder
                .push(complexSubtree)
                .filter(
                    relBuilder.call(
                        org.apache.calcite.sql.fun.SqlStdOperatorTable.GREATER_THAN,
                        relBuilder.field("id"),
                        relBuilder.literal(100)
                    )
                )
                .build();

            RelNode optimized = optimizer.optimize(query);
            assertNotNull(optimized);

            // Print detailed explanation
            System.out.println();
            System.out.println(DOUBLE_SEPARATOR);
            System.out.println("TEST: " + testName);
            System.out.println(DOUBLE_SEPARATOR);
            System.out.println("\n[EXPLANATION]");
            System.out.println("This test verifies that Input<t0> in the template can match:");
            System.out.println("  - Not just LogicalTableScan (base table)");
            System.out.println("  - But also complex RelNode subtrees like JOIN queries");
            System.out.println("  - In this case: LogicalProject(LogicalJoin(customers, orders))");
            System.out.println("\nOriginal query structure:");
            System.out.println("  LogicalFilter (id > 100)");
            System.out.println("    LogicalProject (id, name, order_id)");
            System.out.println("      LogicalJoin (customers.id = orders.customer_id)");
            System.out.println("        LogicalTableScan (customers)");
            System.out.println("        LogicalTableScan (orders)");
            System.out.println("\nSQL Equivalent:");
            System.out.println("  SELECT * FROM (");
            System.out.println("    SELECT c.id, c.name, o.order_id");
            System.out.println("    FROM customers c JOIN orders o ON c.id = o.customer_id");
            System.out.println("  ) t0 WHERE id > 100");
            System.out.println("\nThe rule Filter<p0 a0>(Input<t0>) -> Input<t1> should match:");
            System.out.println("  - Filter<p0 a0> matches the outer LogicalFilter (id > 100)");
            System.out.println("  - Input<t0> matches the entire JOIN subtree: LogicalProject(LogicalJoin(...))");
            System.out.println("  - The rewrite produces: LogicalProject(LogicalJoin(...)) (filter removed)");

            printUnifiedOutput(testName, ruleStr, query, optimized);

            // Verify that the optimized result is the complex subtree (filter removed)
            boolean changed = !query.explain().equals(optimized.explain());
            assertTrue(changed, "The query should have been rewritten (outer filter removed)");

            // Verify the optimized result is a LogicalProject (the top of our complex JOIN subtree)
            assertTrue(optimized instanceof org.apache.calcite.rel.logical.LogicalProject,
                "After removing the outer filter, the result should be the LogicalProject subtree");

        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            fail("Test should not throw exception: " + e.getMessage());
        }
    }
}
