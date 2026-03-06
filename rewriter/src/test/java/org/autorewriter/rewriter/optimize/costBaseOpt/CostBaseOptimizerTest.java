package org.autorewriter.rewriter.optimize.costBaseOpt;

import org.apache.calcite.adapter.jdbc.JdbcConvention;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexSubQuery;
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
import org.autorewriter.rewriter.optimize.trace.RuleApplicationStep;
import org.autorewriter.rewriter.optimize.trace.TraceTreeVisualizer;
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

        Table peopleTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return typeFactory.builder()
                        .add("id", SqlTypeName.INTEGER)
                        .add("diaspora_handle", SqlTypeName.VARCHAR, 200)
                        .add("closed_account", SqlTypeName.BOOLEAN)
                        .build();
            }
        };
        rootSchema.add("people", peopleTable);

        Table contactsTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return typeFactory.builder()
                        .add("id", SqlTypeName.INTEGER)
                        .add("person_id", SqlTypeName.INTEGER)
                        .add("user_id", SqlTypeName.INTEGER)
                        .build();
            }
        };
        rootSchema.add("contacts", contactsTable);

        Table aspectMembershipsTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return typeFactory.builder()
                        .add("id", SqlTypeName.INTEGER)
                        .add("contact_id", SqlTypeName.INTEGER)
                        .add("aspect_id", SqlTypeName.INTEGER)
                        .build();
            }
        };
        rootSchema.add("aspect_memberships", aspectMembershipsTable);

        Table profilesTable = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return typeFactory.builder()
                        .add("id", SqlTypeName.INTEGER)
                        .add("person_id", SqlTypeName.INTEGER)
                        .add("searchable", SqlTypeName.BOOLEAN)
                        .add("full_name", SqlTypeName.VARCHAR, 200)
                        .build();
            }
        };
        rootSchema.add("profiles", profilesTable);

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

    @Test
    public void testJoinCommuteRuleFires() {
        // Build: customers JOIN orders ON customers.id = orders.customer_id
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
        OptimizationTrace trace = new OptimizationTrace();
        RelNode result = optimizer.optimize(query, trace);

        assertNotNull(result);
        assertAllNodesInJdbcConvention(result);

        // Verify JOIN_COMMUTE fired in the trace
        boolean joinCommuteFired = trace.getSteps().stream()
                .anyMatch(step -> step.getRule().getClass().getSimpleName()
                        .contains("JoinCommuteRule"));

        System.out.println(trace.summary());

        assertTrue(joinCommuteFired,
                "Expected JoinCommuteRule to fire but it did not. Trace:\n"
                        + trace.summary());
    }

    @Test
    public void testInSubFilterExposesSubqueryForJoinReordering() {
        relBuilder.clear();

        // Build subquery: orders JOIN customers ON customer_id = id, project customer_id
        RelNode subquery = relBuilder
                .scan("orders")
                .scan("customers")
                .join(JoinRelType.INNER,
                        relBuilder.equals(
                                relBuilder.field(2, 0, "customer_id"),
                                relBuilder.field(2, 1, "id")))
                .project(relBuilder.field("customer_id"))
                .build();

        relBuilder.clear();

        // Build outer: customers WHERE id IN (subquery)
        RelNode outer = relBuilder
                .scan("customers")
                .filter(
                        RexSubQuery.in(subquery,
                                com.google.common.collect.ImmutableList.of(
                                        relBuilder.field("id"))))
                .build();

        CostBaseOptimizer optimizer = new CostBaseOptimizer();
        OptimizationTrace trace = new OptimizationTrace();
        RelNode result = optimizer.optimize(outer, trace);

        assertNotNull(result);
        assertAllNodesInJdbcConvention(result);

        // Verify FilterToInSubFilterRule fired
        boolean inSubFilterRuleFired = trace.getSteps().stream()
                .anyMatch(step -> step.getRule().getClass().getSimpleName()
                        .contains("FilterToInSubFilterRule"));

        System.out.println(trace.summary());

        assertTrue(inSubFilterRuleFired,
                "Expected FilterToInSubFilterRule to fire. Trace:\n" + trace.summary());
    }

    @Test
    public void testNestedInSubFilterWithComplexJoins() {
        // === Build innermost subquery (subquery2) ===
        // SELECT people.id FROM people
        //   JOIN profiles ON profiles.person_id = people.id
        //   JOIN contacts contacts_people ON contacts_people.person_id = people.id
        //   JOIN aspect_memberships ON am.contact_id = contacts_people.id
        //   LEFT JOIN contacts ON contacts.user_id = 488 AND contacts.person_id = people.id
        // WHERE (profiles.searchable = true OR contacts.user_id = 488)
        //   AND (profiles.full_name LIKE '...' OR people.diaspora_handle LIKE '...')
        //   AND people.closed_account = false
        //   AND contacts.user_id = 488
        //   AND aspect_memberships.aspect_id = 321
        relBuilder.clear();

        // people ⋈ profiles ON profiles.person_id = people.id
        relBuilder.scan("people");
        relBuilder.scan("profiles");
        relBuilder.join(JoinRelType.INNER,
                relBuilder.equals(
                        relBuilder.field(2, 0, "id"),
                        relBuilder.field(2, 1, "person_id")));
        // [people.id:0, diaspora_handle:1, closed_account:2,
        //  profiles.id:3, person_id:4, searchable:5, full_name:6]

        // ⋈ contacts (as contacts_people) ON contacts_people.person_id = people.id
        relBuilder.scan("contacts");
        relBuilder.join(JoinRelType.INNER,
                relBuilder.equals(
                        relBuilder.field(2, 0, "id"),
                        relBuilder.field(2, 1, "person_id")));
        // [..., contacts.id:7, contacts.person_id:8, contacts.user_id:9]

        // ⋈ aspect_memberships ON am.contact_id = contacts_people.id
        relBuilder.scan("aspect_memberships");
        relBuilder.join(JoinRelType.INNER,
                relBuilder.equals(
                        relBuilder.field(2, 0, 7),
                        relBuilder.field(2, 1, "contact_id")));
        // [..., am.id:10, am.contact_id:11, am.aspect_id:12]

        // LEFT JOIN contacts ON contacts.user_id = 488 AND contacts.person_id = people.id
        relBuilder.scan("contacts");
        relBuilder.join(JoinRelType.LEFT,
                relBuilder.and(
                        relBuilder.equals(
                                relBuilder.field(2, 1, "user_id"),
                                relBuilder.literal(488)),
                        relBuilder.equals(
                                relBuilder.field(2, 1, "person_id"),
                                relBuilder.field(2, 0, "id"))));
        // [..., contacts2.id:13, contacts2.person_id:14, contacts2.user_id:15]

        // WHERE clause
        relBuilder.filter(
                relBuilder.and(
                        relBuilder.or(
                                relBuilder.equals(relBuilder.field(5), relBuilder.literal(true)),
                                relBuilder.equals(relBuilder.field(15), relBuilder.literal(488))),
                        relBuilder.or(
                                relBuilder.call(SqlStdOperatorTable.LIKE,
                                        relBuilder.field(6), relBuilder.literal("%my% aspect% contact%")),
                                relBuilder.call(SqlStdOperatorTable.LIKE,
                                        relBuilder.field(1), relBuilder.literal("myaspectcontact%"))),
                        relBuilder.equals(relBuilder.field(2), relBuilder.literal(false)),
                        relBuilder.equals(relBuilder.field(15), relBuilder.literal(488)),
                        relBuilder.equals(relBuilder.field(12), relBuilder.literal(321))));

        // PROJECT people.id
        relBuilder.project(relBuilder.field(0));
        RelNode subquery2 = relBuilder.build();

        // === Build middle subquery (subquery1) ===
        // SELECT people.id FROM people
        //   JOIN contacts ON contacts.person_id = people.id
        //   JOIN aspect_memberships ON am.contact_id = contacts.id
        // WHERE people.id IN (subquery2) AND contacts.user_id = 488 AND am.aspect_id = 322
        relBuilder.clear();

        relBuilder.scan("people");
        relBuilder.scan("contacts");
        relBuilder.join(JoinRelType.INNER,
                relBuilder.equals(
                        relBuilder.field(2, 0, "id"),
                        relBuilder.field(2, 1, "person_id")));
        // [people.id:0, diaspora_handle:1, closed_account:2,
        //  contacts.id:3, person_id:4, user_id:5]

        relBuilder.scan("aspect_memberships");
        relBuilder.join(JoinRelType.INNER,
                relBuilder.equals(
                        relBuilder.field(2, 0, 3),
                        relBuilder.field(2, 1, "contact_id")));
        // [..., am.id:6, am.contact_id:7, am.aspect_id:8]

        relBuilder.filter(
                relBuilder.and(
                        RexSubQuery.in(subquery2,
                                com.google.common.collect.ImmutableList.of(relBuilder.field(0))),
                        relBuilder.equals(relBuilder.field(5), relBuilder.literal(488)),
                        relBuilder.equals(relBuilder.field(8), relBuilder.literal(322))));

        relBuilder.project(relBuilder.field(0));
        RelNode subquery1 = relBuilder.build();

        // === Build outer query ===
        // SELECT DISTINCT people.* FROM people WHERE people.id IN (subquery1) LIMIT 15 OFFSET 0
        relBuilder.clear();

        relBuilder.scan("people");
        relBuilder.filter(
                RexSubQuery.in(subquery1,
                        com.google.common.collect.ImmutableList.of(relBuilder.field("id"))));
        relBuilder.distinct();
        relBuilder.sortLimit(0, 15);
        RelNode outerQuery = relBuilder.build();

        // Optimize
        CostBaseOptimizer optimizer = new CostBaseOptimizer();
        OptimizationTrace trace = new OptimizationTrace();
        RelNode result = optimizer.optimize(outerQuery, trace);

        assertNotNull(result);

        // Verify FilterToInSubFilterRule fired (should fire for both IN-subquery levels)
        long inSubFilterRuleCount = trace.getSteps().stream()
                .filter(step -> step.getRule().getClass().getSimpleName()
                        .contains("FilterToInSubFilterRule"))
                .count();

        System.out.println("=== Nested IN-Subquery Optimization Trace ===");
        System.out.println(trace.summary());

        assertTrue(inSubFilterRuleCount >= 1,
                "Expected FilterToInSubFilterRule to fire at least once. Trace:\n"
                        + trace.summary());
    }

    @Test
    public void testExportRewriteTreeAsPng() throws Exception {
        // Use a simple join query to produce a manageable graph
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
        OptimizationTrace trace = new OptimizationTrace();
        optimizer.optimize(query, trace);

        assertTrue(trace.firedCount() > 0);

        // Export DOT (always works)
        String dot = TraceTreeVisualizer.generateDot(trace);
        assertNotNull(dot);
        assertTrue(dot.contains("digraph RewriteTree"));

        // Export PNG (requires Graphviz installed)
        String pngPath = "target/trace-tree.png";
        try {
            trace.exportTreePng(pngPath);
            assertTrue(java.nio.file.Files.exists(java.nio.file.Path.of(pngPath)),
                    "PNG file should be created at " + pngPath);
            System.out.println("Trace tree exported to: "
                    + java.nio.file.Path.of(pngPath).toAbsolutePath());
        } catch (java.io.IOException e) {
            System.out.println("Skipped PNG export (Graphviz not available): " + e.getMessage());
        }
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
