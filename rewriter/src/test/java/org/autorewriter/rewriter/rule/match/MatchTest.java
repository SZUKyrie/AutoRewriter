package org.autorewriter.rewriter.rule.match;

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
import org.autorewriter.rewriter.rule.model.Model;
import org.autorewriter.rewriter.rule.symbol.Symbol;
import org.autorewriter.rewriter.rule.symbol.SymbolKind;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Match} and {@link FilterMatcher}.
 *
 * <p>Uses two RelBuilders:
 * <ul>
 *   <li>{@code templateBuilder} — builds template RelNodes with placeholder names (t0, a0, ...)</li>
 *   <li>{@code queryBuilder} — builds query RelNodes with real tables (customers, orders)</li>
 * </ul>
 */
class MatchTest {

    private RelBuilder templateBuilder;
    private RelBuilder queryBuilder;

    @BeforeEach
    void setup() {
        // ── Template schema (placeholder tables) ──
        SchemaPlus templateSchema = Frameworks.createRootSchema(true);

        // t0 with columns a0, a1 — represents any single-input table placeholder
        templateSchema.add("t0", placeholderTable("a0", "a1"));
        // t1 with columns a2, a3
        templateSchema.add("t1", placeholderTable("a2", "a3"));

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
    // 1. testMatchInputPlaceholder — template Input<t0> matches a LogicalTableScan
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchInputPlaceholder() {
        // Template: scan("t0") — a placeholder table scan
        RelNode template = templateBuilder.scan("t0").build();

        // Query: scan("customers") — a real table scan
        RelNode query = queryBuilder.scan("customers").build();

        Model model = new Model(emptyConstraints());

        boolean result = Match.match(template, query, model);

        assertTrue(result, "Input<t0> should match any LogicalTableScan");

        // Verify t0 is bound
        Symbol t0 = Symbol.of("t0");
        assertTrue(model.isAssigned(t0), "t0 should be assigned");
        assertNotNull(model.ofTable(t0), "t0 should be bound to a RelNode");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. testMatchInputPlaceholderMatchesSubtree — Input<t0> matches a complex Join subtree
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchInputPlaceholderMatchesSubtree() {
        // Template: just Input<t0>
        RelNode template = templateBuilder.scan("t0").build();

        // Query: a complex join subtree
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

        assertTrue(query instanceof LogicalJoin,
                "Query should be a LogicalJoin");

        Model model = new Model(emptyConstraints());

        boolean result = Match.match(template, query, model);

        assertTrue(result, "Input<t0> should match any RelNode subtree including complex joins");

        Symbol t0 = Symbol.of("t0");
        assertTrue(model.isAssigned(t0));
        // The bound value should be the join node
        RelNode bound = model.ofTable(t0);
        assertNotNull(bound);
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. testMatchProject — Proj<a0 s0>(Input<t0>) matches SELECT id FROM customers
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchProject() {
        // Template: Proj<a0>(Input<t0>)
        // Build: scan t0 and project column a0
        templateBuilder.clear();
        RelNode template = templateBuilder
                .scan("t0")
                .project(templateBuilder.field("a0"))
                .build();

        // Query: SELECT id FROM customers
        queryBuilder.clear();
        RelNode query = queryBuilder
                .scan("customers")
                .project(queryBuilder.field("id"))
                .build();

        Model model = new Model(emptyConstraints());

        boolean result = Match.match(template, query, model);

        assertTrue(result, "Proj<a0>(Input<t0>) should match SELECT id FROM customers");

        // Verify t0 is bound to customers scan
        Symbol t0 = Symbol.of("t0");
        assertTrue(model.isAssigned(t0), "t0 should be assigned");

        // Verify a0 is bound to column refs
        Symbol a0 = Symbol.of("a0");
        assertTrue(model.isAssigned(a0), "a0 should be assigned");
        List<ColumnRef> attrs = model.ofAttrs(a0);
        assertNotNull(attrs, "a0 should be bound to column refs");
        assertEquals(1, attrs.size(), "Should have 1 projected column");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. testMatchAggregate — Proj*<a0 s0>(Input<t0>) matches SELECT DISTINCT id FROM customers
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchAggregate() {
        // Template: Proj*<a0>(Input<t0>) = DISTINCT on t0
        // Build: scan t0, project a0, then distinct
        templateBuilder.clear();
        RelNode template = templateBuilder
                .scan("t0")
                .project(templateBuilder.field("a0"))
                .distinct()
                .build();

        // Query: SELECT DISTINCT id FROM customers
        queryBuilder.clear();
        RelNode query = queryBuilder
                .scan("customers")
                .project(queryBuilder.field("id"))
                .distinct()
                .build();

        Model model = new Model(emptyConstraints());

        boolean result = Match.match(template, query, model);

        assertTrue(result, "Proj*<a0>(Input<t0>) should match SELECT DISTINCT id");

        // Verify t0 is bound
        Symbol t0 = Symbol.of("t0");
        assertTrue(model.isAssigned(t0));
    }

    // ══════════════════════════════════════════════════════════════════════
    // 5. testMatchFilter — Filter<p0 a0>(Input<t0>) matches WHERE id > 100
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchFilter() {
        // Template: Filter(Input<t0>) with condition (a0 > 0) — using placeholder ops
        // Since we can't create a RexCall with a custom operator name "p0" via RelBuilder,
        // we test filter matching with the same operator structure.
        // Template: Filter with id > 0 on t0
        templateBuilder.clear();
        RelNode template = templateBuilder
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

        Model model = new Model(emptyConstraints());

        boolean result = Match.match(template, query, model);

        assertTrue(result, "Filter matching should succeed with same operator structure");

        // Verify t0 is bound
        Symbol t0 = Symbol.of("t0");
        assertTrue(model.isAssigned(t0));
    }

    // ══════════════════════════════════════════════════════════════════════
    // 6. testMatchJoin — InnerJoin<a0 a1>(Input<t0>, Input<t1>) matches customers JOIN orders
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchJoin() {
        // Template: InnerJoin(t0, t1) ON t0.a0 = t1.a2
        templateBuilder.clear();
        RelNode template = templateBuilder
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

        Model model = new Model(emptyConstraints());

        boolean result = Match.match(template, query, model);

        assertTrue(result, "InnerJoin template should match customers JOIN orders");

        // Verify both table symbols are bound
        Symbol t0 = Symbol.of("t0");
        Symbol t1 = Symbol.of("t1");
        assertTrue(model.isAssigned(t0), "t0 should be assigned");
        assertTrue(model.isAssigned(t1), "t1 should be assigned");

        // Verify attrs (join keys) are bound
        Symbol a0 = Symbol.of("a0");
        Symbol a2 = Symbol.of("a2");
        assertTrue(model.isAssigned(a0), "a0 (left join key) should be assigned");
        assertTrue(model.isAssigned(a2), "a2 (right join key) should be assigned");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 7. testMatchFilterChain — Filter(Filter(Input)) matches stacked filters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchFilterChain() {
        // Template: Filter(Filter(Input<t0>))
        templateBuilder.clear();
        RelNode template = templateBuilder
                .scan("t0")
                .filter(
                        templateBuilder.call(
                                SqlStdOperatorTable.GREATER_THAN,
                                templateBuilder.field("a0"),
                                templateBuilder.literal(100)
                        )
                )
                .filter(
                        templateBuilder.call(
                                SqlStdOperatorTable.GREATER_THAN,
                                templateBuilder.field("a0"),
                                templateBuilder.literal(100)
                        )
                )
                .build();

        // Query: SELECT * FROM customers WHERE id > 100 (stacked twice)
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
                .filter(
                        queryBuilder.call(
                                SqlStdOperatorTable.GREATER_THAN,
                                queryBuilder.field("id"),
                                queryBuilder.literal(100)
                        )
                )
                .build();

        Model model = new Model(emptyConstraints());

        boolean result = Match.match(template, query, model);

        assertTrue(result, "Filter chain template should match stacked query filters");

        Symbol t0 = Symbol.of("t0");
        assertTrue(model.isAssigned(t0), "t0 should be assigned after filter chain match");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 8. testModelBacktracking — verify derive() + discard works
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testModelBacktracking() {
        // Create a model and make an assignment in a derived model
        Model parent = new Model(emptyConstraints());
        Symbol t0 = Symbol.of("t0");

        queryBuilder.clear();
        RelNode customersScan = queryBuilder.scan("customers").build();
        parent.assign(t0, customersScan);

        // Derive a child model
        Model child = parent.derive();

        // Child should see parent's t0
        assertTrue(child.isAssigned(t0), "Child should see parent's assignment");
        assertSame(customersScan, child.ofTable(t0));

        // Make a new assignment in child
        Symbol t1 = Symbol.of("t1");
        queryBuilder.clear();
        RelNode ordersScan = queryBuilder.scan("orders").build();
        child.assign(t1, ordersScan);

        // Child sees both
        assertTrue(child.isAssigned(t1));

        // Parent does NOT see child's assignment
        assertFalse(parent.isAssigned(t1),
                "Parent should not see child's assignment — derive() provides isolation");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 9. testMatchFailsOnTypeMismatch — template Proj doesn't match query Filter
    //    (transparent Proj matching only activates inside Join child context)
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchFailsOnTypeMismatch() {
        // Template: a project
        templateBuilder.clear();
        RelNode template = templateBuilder
                .scan("t0")
                .project(templateBuilder.field("a0"))
                .build();

        // Query: a filter (not a project)
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

        Model model = new Model(emptyConstraints());

        boolean result = Match.match(template, query, model);

        assertFalse(result, "Project template should not match a Filter query outside Join context");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 10. testMatchJoinTypeMismatch — LEFT JOIN template doesn't match INNER JOIN query
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchJoinTypeMismatch() {
        // Template: LEFT JOIN
        templateBuilder.clear();
        RelNode template = templateBuilder
                .scan("t0")
                .scan("t1")
                .join(JoinRelType.LEFT,
                        templateBuilder.equals(
                                templateBuilder.field(2, 0, "a0"),
                                templateBuilder.field(2, 1, "a2")
                        ))
                .build();

        // Query: INNER JOIN
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

        Model model = new Model(emptyConstraints());

        boolean result = Match.match(template, query, model);

        assertFalse(result, "LEFT JOIN template should not match INNER JOIN query");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 11. testMatchWithConstraints — ATTRS_SUB constraint integration
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchWithConstraints() {
        // Template: Proj<a0>(Input<t0>)
        templateBuilder.clear();
        RelNode template = templateBuilder
                .scan("t0")
                .project(templateBuilder.field("a0"))
                .build();

        // Query: SELECT id FROM customers
        queryBuilder.clear();
        RelNode query = queryBuilder
                .scan("customers")
                .project(queryBuilder.field("id"))
                .build();

        // Build constraints: ATTRS_SUB(a0, t0)
        Symbol a0 = Symbol.of("a0");
        Symbol t0 = Symbol.of("t0");
        Constraint attrsSub = Constraint.of(ConstraintKind.ATTRS_SUB, a0, t0);
        Constraints constraints = Constraints.build(
                Collections.singletonList(attrsSub),
                setOf(a0, t0),
                setOf()
        );

        Model model = new Model(constraints);

        boolean result = Match.match(template, query, model);

        assertTrue(result, "Match with ATTRS_SUB constraint should succeed when attrs are valid subset");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 12. testMatchProjectMultipleColumns — match project with multiple columns
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchProjectMultipleColumns() {
        // Template: Proj<a0, a1>(Input<t0>) — project both columns of t0
        templateBuilder.clear();
        RelNode template = templateBuilder
                .scan("t0")
                .project(
                        templateBuilder.field("a0"),
                        templateBuilder.field("a1")
                )
                .build();

        // Query: SELECT id, name FROM customers
        queryBuilder.clear();
        RelNode query = queryBuilder
                .scan("customers")
                .project(
                        queryBuilder.field("id"),
                        queryBuilder.field("name")
                )
                .build();

        Model model = new Model(emptyConstraints());

        boolean result = Match.match(template, query, model);

        assertTrue(result, "Proj with 2 columns should match query with 2 columns");

        Symbol t0 = Symbol.of("t0");
        assertTrue(model.isAssigned(t0));
    }

    // ══════════════════════════════════════════════════════════════════════
    // 13. testMatchNestedProjectFilter — Proj(Filter(Input))
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchNestedProjectFilter() {
        // Template: Proj<a0>(Filter(Input<t0>))
        templateBuilder.clear();
        RelNode template = templateBuilder
                .scan("t0")
                .filter(
                        templateBuilder.call(
                                SqlStdOperatorTable.GREATER_THAN,
                                templateBuilder.field("a0"),
                                templateBuilder.literal(100)
                        )
                )
                .project(templateBuilder.field("a0"))
                .build();

        // Query: SELECT id FROM customers WHERE id > 100
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
                .project(queryBuilder.field("id"))
                .build();

        Model model = new Model(emptyConstraints());

        boolean result = Match.match(template, query, model);

        assertTrue(result, "Proj(Filter(Input)) should match SELECT id FROM customers WHERE id > 100");

        Symbol t0 = Symbol.of("t0");
        assertTrue(model.isAssigned(t0));
    }

    // ══════════════════════════════════════════════════════════════════════
    // 14. testUnwrapHepVertex
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testUnwrapHepVertex() {
        // Non-HepRelVertex should be returned as-is
        queryBuilder.clear();
        RelNode scan = queryBuilder.scan("customers").build();

        RelNode unwrapped = Match.unwrapHepVertex(scan);
        assertSame(scan, unwrapped, "Non-HepRelVertex should be returned as-is");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 15. testMatchExactTableScan — non-placeholder table names must match exactly
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchExactTableScan() {
        // Template with a real (non-placeholder) table name
        // We'll use the query builder for both since we need real tables
        queryBuilder.clear();
        RelNode template = queryBuilder.scan("customers").build();
        queryBuilder.clear();
        RelNode queryMatch = queryBuilder.scan("customers").build();
        queryBuilder.clear();
        RelNode queryNoMatch = queryBuilder.scan("orders").build();

        Model model1 = new Model(emptyConstraints());
        assertTrue(Match.match(template, queryMatch, model1),
                "Same table name should match");

        Model model2 = new Model(emptyConstraints());
        assertFalse(Match.match(template, queryNoMatch, model2),
                "Different table name should not match");
    }

    // ══════════════════════════════════════════════════════════════════════
    // 16. testMatchAggregateNonDistinctFails
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void testMatchAggregateNonDistinctFails() {
        // Template: DISTINCT (empty agg calls)
        templateBuilder.clear();
        RelNode template = templateBuilder
                .scan("t0")
                .project(templateBuilder.field("a0"))
                .distinct()
                .build();

        // Query: GROUP BY with aggregate function (not just DISTINCT)
        queryBuilder.clear();
        RelNode query = queryBuilder
                .scan("customers")
                .aggregate(
                        queryBuilder.groupKey("id"),
                        queryBuilder.count(false, "cnt")
                )
                .build();

        Model model = new Model(emptyConstraints());

        boolean result = Match.match(template, query, model);

        assertFalse(result, "DISTINCT template should not match GROUP BY with aggregate functions");
    }
}
