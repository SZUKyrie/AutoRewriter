package org.autorewriter.rewriter.rule.instantiation;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.logical.*;
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
import org.autorewriter.rewriter.rule.constraint.Constraint;
import org.autorewriter.rewriter.rule.constraint.ConstraintKind;
import org.autorewriter.rewriter.rule.constraint.Constraints;
import org.autorewriter.rewriter.rule.match.Match;
import org.autorewriter.rewriter.rule.model.Model;
import org.autorewriter.rewriter.rule.symbol.Symbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Instantiation}.
 *
 * <p>Each test builds a source template + query, runs {@link Match#match} to populate
 * a {@link Model}, then calls {@link Instantiation#instantiate} on a target template
 * and verifies the resulting RelNode structure.
 *
 * <p>Uses two RelBuilders:
 * <ul>
 *   <li>{@code templateBuilder} — builds template RelNodes with placeholder names</li>
 *   <li>{@code queryBuilder} — builds query RelNodes with real tables (customers, orders)</li>
 * </ul>
 */
class InstantiationTest {

    private RelBuilder templateBuilder;
    private RelBuilder queryBuilder;

    @BeforeEach
    void setup() {
        // ── Template schema (placeholder tables) ──
        SchemaPlus templateSchema = Frameworks.createRootSchema(true);
        templateSchema.add("t0", placeholderTable("a0", "a1"));
        templateSchema.add("t1", placeholderTable("a2", "a3"));
        templateSchema.add("t2", placeholderTable("a4", "a5"));
        templateSchema.add("t3", placeholderTable("a6", "a7"));

        FrameworkConfig templateConfig = Frameworks.newConfigBuilder()
                .defaultSchema(templateSchema)
                .build();
        templateBuilder = RelBuilder.create(templateConfig);

        // ── Query schema (real tables) ──
        SchemaPlus querySchema = Frameworks.createRootSchema(true);

        // customers(id INT NOT NULL, name VARCHAR, email VARCHAR)
        querySchema.add("customers", new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return typeFactory.builder()
                        .add("id", typeFactory.createTypeWithNullability(
                                typeFactory.createSqlType(SqlTypeName.INTEGER), false))
                        .add("name", typeFactory.createTypeWithNullability(
                                typeFactory.createSqlType(SqlTypeName.VARCHAR, 100), true))
                        .add("email", typeFactory.createTypeWithNullability(
                                typeFactory.createSqlType(SqlTypeName.VARCHAR, 200), true))
                        .build();
            }
        });

        // orders(order_id INT NOT NULL, customer_id INT, amount DECIMAL, order_date DATE)
        querySchema.add("orders", new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return typeFactory.builder()
                        .add("order_id", typeFactory.createTypeWithNullability(
                                typeFactory.createSqlType(SqlTypeName.INTEGER), false))
                        .add("customer_id", typeFactory.createTypeWithNullability(
                                typeFactory.createSqlType(SqlTypeName.INTEGER), true))
                        .add("amount", typeFactory.createTypeWithNullability(
                                typeFactory.createSqlType(SqlTypeName.DECIMAL, 10, 2), true))
                        .add("order_date", typeFactory.createTypeWithNullability(
                                typeFactory.createSqlType(SqlTypeName.DATE), true))
                        .build();
            }
        });

        FrameworkConfig queryConfig = Frameworks.newConfigBuilder()
                .defaultSchema(querySchema)
                .build();
        queryBuilder = RelBuilder.create(queryConfig);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static Table placeholderTable(String col0, String col1) {
        return new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return typeFactory.builder()
                        .add(col0, SqlTypeName.INTEGER)
                        .add(col1, SqlTypeName.INTEGER)
                        .build();
            }
        };
    }

    private static Set<Symbol> setOf(Symbol... symbols) {
        return new HashSet<>(Arrays.asList(symbols));
    }

    private Constraints emptyConstraints() {
        return Constraints.build(Collections.emptyList(), setOf(), setOf());
    }

    // ══════════════════════════════════════════════════════════════════════
    // 1. testInstantiateInput — target template Input<t1> with TableEq(t1,t0),
    //    t0 bound to customers table → returns customers scan
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testInstantiateInput() {
        // --- Source side ---
        // Source template: Input<t0> (matches any subtree)
        templateBuilder.clear();
        RelNode sourceTemplate = templateBuilder.scan("t0").build();

        // Query: scan("customers")
        queryBuilder.clear();
        RelNode query = queryBuilder.scan("customers").build();

        // Constraints: TableEq(t1, t0) — t0 is source, t1 is target
        Symbol t0 = Symbol.of("t0");
        Symbol t1 = Symbol.of("t1");
        Constraint tableEq = Constraint.of(ConstraintKind.TABLE_EQ, t1, t0);
        Constraints constraints = Constraints.build(
                Collections.singletonList(tableEq),
                setOf(t0),     // source symbols
                setOf(t1)      // target symbols
        );

        // Match source template against query to populate model
        Model model = new Model(constraints);
        boolean matched = Match.match(sourceTemplate, query, model);
        assertTrue(matched, "Source template should match query");

        // --- Target side ---
        // Target template: Input<t1>
        templateBuilder.clear();
        RelNode targetTemplate = templateBuilder.scan("t1").build();

        // Instantiate
        RelNode result = Instantiation.instantiate(targetTemplate, model, constraints, targetTemplate.getCluster());

        // Verify: result should be the customers scan
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof LogicalTableScan,
                "Result should be a LogicalTableScan, got: " + result.getClass().getSimpleName());
        String tableName = result.getTable().getQualifiedName()
                .get(result.getTable().getQualifiedName().size() - 1);
        assertEquals("customers", tableName,
                "Instantiated table should be 'customers'");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. testInstantiateProject — target template Proj<a1 s1>(Input<t1>)
    //    with TableEq(t1,t0), AttrsEq(a1,a0), SchemaEq(s1,s0)
    //    → returns LogicalProject with correct columns
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testInstantiateProject() {
        // --- Source side ---
        // Source template: Proj<a0>(Input<t0>)
        templateBuilder.clear();
        RelNode sourceTemplate = templateBuilder
                .scan("t0")
                .project(templateBuilder.field("a0"))
                .build();

        // Query: SELECT id FROM customers
        queryBuilder.clear();
        RelNode query = queryBuilder
                .scan("customers")
                .project(queryBuilder.field("id"))
                .build();

        // Constraints:
        //   TableEq(t1, t0) — cross-side
        //   AttrsEq(a1, a0) — cross-side
        //   SchemaEq(s1, s0) — cross-side
        Symbol t0 = Symbol.of("t0");
        Symbol a0 = Symbol.of("a0");
        Symbol s0 = Symbol.of("s0");
        Symbol t1 = Symbol.of("t1");
        Symbol a1 = Symbol.of("a1");
        Symbol s1 = Symbol.of("s1");

        List<Constraint> constraintList = Arrays.asList(
                Constraint.of(ConstraintKind.TABLE_EQ, t1, t0),
                Constraint.of(ConstraintKind.ATTRS_EQ, a1, a0),
                Constraint.of(ConstraintKind.SCHEMA_EQ, s1, s0)
        );
        Constraints constraints = Constraints.build(
                constraintList,
                setOf(t0, a0, s0),     // source symbols
                setOf(t1, a1, s1)      // target symbols
        );

        // Match
        Model model = new Model(constraints);
        boolean matched = Match.match(sourceTemplate, query, model);
        assertTrue(matched, "Source template should match query");

        // --- Target side ---
        // Target template: Proj<a1>(Input<t1>)
        // We need a template schema with t1 having column a1
        templateBuilder.clear();
        RelNode targetTemplate = templateBuilder
                .scan("t1")
                .project(templateBuilder.field("a2"))  // a2 is the first column of t1
                .build();
        // Note: The target template field names don't matter much because Instantiation
        // looks at the template's field names for placeholder detection. Since the template
        // schema uses a2/a3 for t1, and we used a1 in constraints, we need to build a custom target.

        // Actually, let's build a simpler approach: use the same template builder with t1's field names
        // The field name in the project output becomes the placeholder.
        // Since templateBuilder's t1 has fields a2, a3, let's use a1 mapped from source a0.
        // The key insight: the TEMPLATE's output field names are what matters.
        // For the project template, the first column name in output IS the attrs placeholder.

        // Let's rebuild with a custom target template schema that has t1 with columns a1, s1
        SchemaPlus targetSchema = Frameworks.createRootSchema(true);
        targetSchema.add("t1", placeholderTable("a1", "s1"));
        FrameworkConfig targetConfig = Frameworks.newConfigBuilder()
                .defaultSchema(targetSchema)
                .build();
        RelBuilder targetBuilder = RelBuilder.create(targetConfig);
        targetTemplate = targetBuilder
                .scan("t1")
                .project(targetBuilder.field("a1"))
                .build();

        // Instantiate
        RelNode result = Instantiation.instantiate(targetTemplate, model, constraints, targetTemplate.getCluster());

        // Verify: result should be a LogicalProject
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof LogicalProject,
                "Result should be a LogicalProject, got: " + result.getClass().getSimpleName());

        LogicalProject proj = (LogicalProject) result;
        // The projection should have 1 column
        assertEquals(1, proj.getProjects().size(),
                "Project should have 1 projected column");

        // The child should be the customers scan
        RelNode projChild = Match.unwrapHepVertex(proj.getInput());
        assertTrue(projChild instanceof LogicalTableScan,
                "Project child should be LogicalTableScan");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. testInstantiateFilter — target template Filter<p1 a1>(Input<t1>)
    //    with bound predicate → returns LogicalFilter
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testInstantiateFilter() {
        // --- Source side ---
        // Source template: Filter(Input<t0>) with condition (a0 > 100)
        templateBuilder.clear();
        RelNode sourceTemplate = templateBuilder
                .scan("t0")
                .filter(
                        templateBuilder.call(
                                SqlStdOperatorTable.GREATER_THAN,
                                templateBuilder.field("a0"),
                                templateBuilder.literal(100)
                        )
                )
                .build();

        // Query: SELECT * FROM customers WHERE id > 100
        queryBuilder.clear();
        RelNode query = queryBuilder
                .scan("customers")
                .filter(
                        queryBuilder.call(
                                SqlStdOperatorTable.GREATER_THAN,
                                queryBuilder.field("id"),
                                queryBuilder.literal(100)
                        )
                )
                .build();

        // Constraints: TableEq(t1, t0) — cross-side
        Symbol t0 = Symbol.of("t0");
        Symbol t1 = Symbol.of("t1");

        List<Constraint> constraintList = Collections.singletonList(
                Constraint.of(ConstraintKind.TABLE_EQ, t1, t0)
        );
        Constraints constraints = Constraints.build(
                constraintList,
                setOf(t0),     // source symbols
                setOf(t1)      // target symbols
        );

        // Match
        Model model = new Model(constraints);
        boolean matched = Match.match(sourceTemplate, query, model);
        assertTrue(matched, "Source template should match query");

        // --- Target side ---
        // Target template: Filter(Input<t1>) with same condition shape
        SchemaPlus targetSchema = Frameworks.createRootSchema(true);
        targetSchema.add("t1", placeholderTable("a1", "s1"));
        FrameworkConfig targetConfig = Frameworks.newConfigBuilder()
                .defaultSchema(targetSchema)
                .build();
        RelBuilder targetBuilder = RelBuilder.create(targetConfig);

        RelNode targetTemplate = targetBuilder
                .scan("t1")
                .filter(
                        targetBuilder.call(
                                SqlStdOperatorTable.GREATER_THAN,
                                targetBuilder.field("a1"),
                                targetBuilder.literal(100)
                        )
                )
                .build();

        // Instantiate
        RelNode result = Instantiation.instantiate(targetTemplate, model, constraints, targetTemplate.getCluster());

        // Verify: result should be a LogicalFilter
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof LogicalFilter,
                "Result should be a LogicalFilter, got: " + result.getClass().getSimpleName());

        LogicalFilter filter = (LogicalFilter) result;
        // The child should be the customers scan (instantiated from t1 → t0)
        RelNode filterChild = Match.unwrapHepVertex(filter.getInput());
        assertTrue(filterChild instanceof LogicalTableScan,
                "Filter child should be LogicalTableScan");

        // Verify the condition is present
        assertNotNull(filter.getCondition(), "Filter condition should not be null");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. testInstantiateJoin — target template
    //    InnerJoin<a3 a4>(Input<t2>, Input<t3>)
    //    → returns LogicalJoin with correct condition
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testInstantiateJoin() {
        // --- Source side ---
        // Source template: InnerJoin(Input<t0>, Input<t1>) ON t0.a0 = t1.a2
        templateBuilder.clear();
        RelNode sourceTemplate = templateBuilder
                .scan("t0")
                .scan("t1")
                .join(JoinRelType.INNER,
                        templateBuilder.equals(
                                templateBuilder.field(2, 0, "a0"),
                                templateBuilder.field(2, 1, "a2")
                        ))
                .build();

        // Query: customers JOIN orders ON customers.id = orders.customer_id
        queryBuilder.clear();
        RelNode query = queryBuilder
                .scan("customers")
                .scan("orders")
                .join(JoinRelType.INNER,
                        queryBuilder.equals(
                                queryBuilder.field(2, 0, "id"),
                                queryBuilder.field(2, 1, "customer_id")
                        ))
                .build();

        // Constraints:
        //   TableEq(t2, t0), TableEq(t3, t1) — cross-side table mappings
        //   AttrsEq(a4, a0), AttrsEq(a6, a2) — cross-side attrs mappings
        Symbol t0 = Symbol.of("t0");
        Symbol t1 = Symbol.of("t1");
        Symbol a0 = Symbol.of("a0");
        Symbol a2 = Symbol.of("a2");
        Symbol t2 = Symbol.of("t2");
        Symbol t3 = Symbol.of("t3");
        Symbol a4 = Symbol.of("a4");
        Symbol a6 = Symbol.of("a6");

        List<Constraint> constraintList = Arrays.asList(
                Constraint.of(ConstraintKind.TABLE_EQ, t2, t0),
                Constraint.of(ConstraintKind.TABLE_EQ, t3, t1),
                Constraint.of(ConstraintKind.ATTRS_EQ, a4, a0),
                Constraint.of(ConstraintKind.ATTRS_EQ, a6, a2)
        );
        Constraints constraints = Constraints.build(
                constraintList,
                setOf(t0, t1, a0, a2),           // source symbols
                setOf(t2, t3, a4, a6)             // target symbols
        );

        // Match
        Model model = new Model(constraints);
        boolean matched = Match.match(sourceTemplate, query, model);
        assertTrue(matched, "Source template should match query");

        // --- Target side ---
        // Target template: InnerJoin(Input<t2>, Input<t3>) ON t2.a4 = t3.a6
        SchemaPlus targetSchema = Frameworks.createRootSchema(true);
        targetSchema.add("t2", placeholderTable("a4", "a5"));
        targetSchema.add("t3", placeholderTable("a6", "a7"));
        FrameworkConfig targetConfig = Frameworks.newConfigBuilder()
                .defaultSchema(targetSchema)
                .build();
        RelBuilder targetBuilder = RelBuilder.create(targetConfig);

        RelNode targetTemplate = targetBuilder
                .scan("t2")
                .scan("t3")
                .join(JoinRelType.INNER,
                        targetBuilder.equals(
                                targetBuilder.field(2, 0, "a4"),
                                targetBuilder.field(2, 1, "a6")
                        ))
                .build();

        // Instantiate
        RelNode result = Instantiation.instantiate(targetTemplate, model, constraints, targetTemplate.getCluster());

        // Verify: result should be a LogicalJoin
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof LogicalJoin,
                "Result should be a LogicalJoin, got: " + result.getClass().getSimpleName());

        LogicalJoin join = (LogicalJoin) result;
        assertEquals(JoinRelType.INNER, join.getJoinType(),
                "Join type should be INNER");

        // Verify left and right children are actual table scans
        RelNode leftChild = Match.unwrapHepVertex(join.getLeft());
        RelNode rightChild = Match.unwrapHepVertex(join.getRight());
        assertTrue(leftChild instanceof LogicalTableScan,
                "Left child should be LogicalTableScan");
        assertTrue(rightChild instanceof LogicalTableScan,
                "Right child should be LogicalTableScan");

        // Verify the join condition is not null/trivial
        assertNotNull(join.getCondition(), "Join condition should not be null");
    }
}
