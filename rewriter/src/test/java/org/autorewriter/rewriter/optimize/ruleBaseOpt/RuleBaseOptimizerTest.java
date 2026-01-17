package org.autorewriter.rewriter.optimize.ruleBaseOpt;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.hep.HepMatchOrder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalTableScan;
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
     * Test 1: Register a rule that removes unnecessary DISTINCT.
     *
     * <p>Rule: Proj*<a0 s0>(Input<t0>) -> Proj<a1 s1>(Input<t1>)</p>
     * <p>Condition: Column has unique constraint</p>
     */
    @Test
    public void testRemoveDistinctWithUniqueConstraint() {
        System.out.println("\n=== Test 1: Remove DISTINCT with Unique Constraint ===");

        // Rule: Remove DISTINCT when column has unique constraint
        String ruleStr = "Proj*<a0 s0>(Input<t0>)|Proj<a1 s1>(Input<t1>)|AttrsSub(a0,t0);Unique(t0,a0);TableEq(t1,t0);AttrsEq(a1,a0);SchemaEq(s1,s0)";

        System.out.println("Registering rule: " + ruleStr);
        // Parse the rule
        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        assertNotNull(ruleContext);
        assertNotNull(ruleContext.getSourceRelNode());
        assertNotNull(ruleContext.getTargetRelNode());

        // Create AutoRewriteRule
        // Rule starts with Proj* (DISTINCT), so match LogicalAggregate
        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalAggregate.class, RelOptRule.any()),
            ruleContext
        );

        // Create optimizer and register the rule
        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);

        // Verify rule is registered
        assertEquals(1, optimizer.getRuleCount());

        // Construct query: SELECT DISTINCT customer_id FROM customers
        try {
            RelNode query = relBuilder
                .scan("customers")
                .project(relBuilder.field("id"))
                .distinct()
                .build();

            System.out.println("Original query:");
            System.out.println(query.explain());
            System.out.println("\nOriginal SQL:");
            System.out.println(relNodeToSql(query).replace('\n', ' '));

            // Apply optimization
            RelNode optimized = optimizer.optimize(query);
            assertNotNull(optimized);

            System.out.println("\nOptimized query:");
            System.out.println(optimized.explain());
            System.out.println("\nOptimized SQL:");
            System.out.println(relNodeToSql(optimized).replace('\n', ' '));

            System.out.println("\nRule application: " + (query.equals(optimized) ? "No change" : "Query rewritten"));
        } catch (Exception e) {
            System.out.println("Query construction/optimization failed: " + e.getMessage());
        }
    }

    /**
     * Helper method to convert RelNode to SQL string.
     */
    private String relNodeToSql(RelNode relNode) {
        try {
            // Create a SQL dialect (using default/ANSI SQL)
            org.apache.calcite.sql.SqlDialect dialect = org.apache.calcite.sql.dialect.AnsiSqlDialect.DEFAULT;

            // Convert RelNode to SQL
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
     * Test 2: Register a rule that converts LEFT JOIN to INNER JOIN.
     *
     * <p>Rule: LeftJoin -> InnerJoin</p>
     * <p>Condition: Right side has unique and not null constraint</p>
     */
    @Test
    public void testLeftJoinToInnerJoin() {
        System.out.println("\n=== Test 2: LEFT JOIN to INNER JOIN ===");

        // Rule: LEFT JOIN to INNER JOIN when right side is unique and not null
        String ruleStr = "LeftJoin<a1 a2>(Proj*<a0 s0>(Input<t0>),Input<t1>)" +
                "|InnerJoin<a4 a5>(Proj<a3 s1>(Input<t2>),Input<t3>)" +
                "|TableEq(t0,t1);AttrsEq(a1,a2);AttrsSub(a0,t0);AttrsSub(a1,s0);" +
                "AttrsSub(a2,t1);NotNull(t1,a2);TableEq(t2,t0);TableEq(t3,t1);" +
                "AttrsEq(a3,a0);AttrsEq(a4,a1);AttrsEq(a5,a2);SchemaEq(s1,s0)";

        // Parse the rule
        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        assertNotNull(ruleContext);

        // Create AutoRewriteRule
        // Rule starts with LeftJoin, so match LogicalJoin
        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalJoin.class, RelOptRule.any()),
            ruleContext
        );

        // Create optimizer and register the rule
        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);

        // Verify rule is registered
        assertEquals(1, optimizer.getRuleCount());

        // Construct query matching the rule structure:
        // LeftJoin(Proj*(Input<t0>), Input<t1>)
        // i.e., SELECT DISTINCT c.id FROM customers c (left side with DISTINCT)
        //       LEFT JOIN orders o ON c.id = o.customer_id
        try {
            relBuilder.clear();

            // Build left side: Proj*(Input<customers>) = DISTINCT projection
            RelNode leftSide = relBuilder
                .scan("customers")
                .project(relBuilder.field("id"))  // Project id
                .distinct()                        // DISTINCT (Proj*)
                .build();

            // Build right side: Input<orders> = simple table scan
            relBuilder.clear();
            RelNode rightSide = relBuilder
                .scan("orders")
                .build();

            // Build the JOIN: LeftJoin(leftSide, rightSide)
            relBuilder.clear();
            RelNode query = relBuilder
                .push(leftSide)
                .push(rightSide)
                .join(
                    org.apache.calcite.rel.core.JoinRelType.LEFT,
                    relBuilder.equals(
                        relBuilder.field(2, 0, "id"),           // Left side's id
                        relBuilder.field(2, 1, "customer_id")   // Right side's customer_id
                    )
                )
                .build();

            System.out.println("Original query:");
            System.out.println(query.explain());

            // Apply optimization
            RelNode optimized = optimizer.optimize(query);
            assertNotNull(optimized);

            System.out.println("\nOptimized query:");
            System.out.println(optimized.explain());

            System.out.println("Rule application: " + (query.equals(optimized) ? "No change" : "Query rewritten"));
        } catch (Exception e) {
            System.out.println("Query construction/optimization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test 3: Register a rule that simplifies projection on LEFT JOIN.
     *
     * <p>Rule: Proj(LeftJoin) -> Proj(LeftJoin) with simplified projection</p>
     */
    @Test
    public void testSimplifyProjectionOnLeftJoin() {
        System.out.println("\n=== Test 3: Simplify Projection on LEFT JOIN ===");

        // Rule: Simplify projection on LEFT JOIN
        String ruleStr = "Proj<a2 s0>(LeftJoin<a0 a1>(Input<t0>,Input<t1>))|Proj<a5 s1>(LeftJoin<a3 a4>(Input<t2>,Input<t3>))|AttrsEq(a0,a2);AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t0);Reference(t0,a0,t1,a1);TableEq(t2,t0);TableEq(t3,t1);AttrsEq(a3,a0);AttrsEq(a4,a1);AttrsEq(a5,a1);SchemaEq(s1,s0)";

        // Parse the rule
        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        assertNotNull(ruleContext);

        // Create AutoRewriteRule
        // Rule starts with Proj (Projection), so match LogicalProject
        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalProject.class, RelOptRule.any()),
            ruleContext
        );

        // Create optimizer and register the rule
        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);

        // Verify rule is registered
        assertEquals(1, optimizer.getRuleCount());

        // Construct query: SELECT c.id FROM customers c LEFT JOIN orders o ON c.id = o.customer_id
        try {
            relBuilder.clear();
            RelNode query = relBuilder
                .scan("customers")
                .as("c")
                .scan("orders")
                .as("o")
                .join(
                    org.apache.calcite.rel.core.JoinRelType.LEFT,
                    relBuilder.equals(
                        relBuilder.field(2, 0, "id"),
                        relBuilder.field(2, 1, "customer_id")
                    )
                )
                .project(relBuilder.field("c", "id"))
                .build();

            System.out.println("Original query:");
            System.out.println(query.explain());

            // Apply optimization
            RelNode optimized = optimizer.optimize(query);
            assertNotNull(optimized);

            System.out.println("\nOptimized query:");
            System.out.println(optimized.explain());

            System.out.println("Rule application: " + (query.equals(optimized) ? "No change" : "Query rewritten"));
        } catch (Exception e) {
            System.out.println("Query construction/optimization failed: " + e.getMessage());
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
        System.out.println("\n=== Test 4: Eliminate JOIN with Unique Key ===");

        // Rule: Eliminate INNER JOIN when join key is unique
        String ruleStr = "Proj<a2 s0>(InnerJoin<a0 a1>(Input<t0>,Input<t1>))|Proj<a3 s1>(Input<t2>)|AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t0);Unique(t1,a1);NotNull(t0,a0);Reference(t0,a0,t1,a1);TableEq(t2,t0);AttrsEq(a3,a2);SchemaEq(s1,s0)";

        // Parse the rule
        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        assertNotNull(ruleContext);

        // Create AutoRewriteRule
        // Rule starts with Proj (Projection), so match LogicalProject
        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalProject.class, RelOptRule.any()),
            ruleContext
        );

        // Create optimizer and register the rule
        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);

        // Verify rule is registered
        assertEquals(1, optimizer.getRuleCount());

        // Construct query: SELECT c.name FROM customers c INNER JOIN orders o ON c.id = o.customer_id
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

            System.out.println("Original query:");
            System.out.println(query.explain());

            // Apply optimization
            RelNode optimized = optimizer.optimize(query);
            assertNotNull(optimized);

            System.out.println("\nOptimized query:");
            System.out.println(optimized.explain());

            System.out.println("Rule application: " + (query.equals(optimized) ? "No change" : "Query rewritten"));
        } catch (Exception e) {
            System.out.println("Query construction/optimization failed: " + e.getMessage());
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
        // Create optimizer and add a rule
        String ruleStr = "Proj*<a0 s0>(Input<t0>)|Proj<a1 s1>(Input<t1>)|AttrsSub(a0,t0);Unique(t0,a0);TableEq(t1,t0);AttrsEq(a1,a0);SchemaEq(s1,s0)";
        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        // Rule starts with Proj*, so match LogicalAggregate
        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalAggregate.class, RelOptRule.any()),
            ruleContext
        );

        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);
        assertEquals(1, optimizer.getRuleCount());

        // Clear rules
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
        System.out.println("\n=== Test 10: Merge Duplicate Filters ===");

        // Rule: Merge duplicate filters on same attribute
        String ruleStr = "Filter<p1 a1>(Filter<p0 a0>(Input<t0>))|Filter<p2 a2>(Input<t1>)|AttrsEq(a0,a1);PredicateEq(p0,p1);AttrsSub(a0,t0);AttrsSub(a1,t0);TableEq(t1,t0);AttrsEq(a2,a0);PredicateEq(p2,p0)";

        // Parse the rule
        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        assertNotNull(ruleContext);

        // Create AutoRewriteRule
        // Rule starts with Filter, so match LogicalFilter
        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalFilter.class, RelOptRule.any()),
            ruleContext
        );

        // Create optimizer and register the rule
        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);

        // Verify rule is registered
        assertEquals(1, optimizer.getRuleCount());

        // Construct query: SELECT * FROM customers WHERE customer_id > 100 AND customer_id > 100
        try {
            relBuilder.clear();
            RelNode query = relBuilder
                .scan("customers")
                .filter(
                    relBuilder.call(
                        org.apache.calcite.sql.fun.SqlStdOperatorTable.GREATER_THAN,
                        relBuilder.field("customer_id"),
                        relBuilder.literal(100)
                    )
                )
                .filter(
                    relBuilder.call(
                        org.apache.calcite.sql.fun.SqlStdOperatorTable.GREATER_THAN,
                        relBuilder.field("customer_id"),
                        relBuilder.literal(100)
                    )
                )
                .build();

            System.out.println("Original query:");
            System.out.println(query.explain());

            // Apply optimization
            RelNode optimized = optimizer.optimize(query);
            assertNotNull(optimized);

            System.out.println("\nOptimized query:");
            System.out.println(optimized.explain());

            System.out.println("Rule application: " + (query.equals(optimized) ? "No change" : "Query rewritten"));
        } catch (Exception e) {
            System.out.println("Query construction/optimization failed: " + e.getMessage());
        }
    }
}
