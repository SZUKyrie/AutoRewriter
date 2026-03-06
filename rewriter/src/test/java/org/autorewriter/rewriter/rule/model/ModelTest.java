package org.autorewriter.rewriter.rule.model;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexNode;
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
import org.autorewriter.rewriter.rule.symbol.Symbol;
import org.autorewriter.rewriter.rule.symbol.SymbolKind;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Model}.
 *
 * <p>Uses Calcite's RelBuilder to create real RelNodes for testing.
 */
class ModelTest {

    private RelBuilder relBuilder;

    @BeforeEach
    void setup() {
        SchemaPlus rootSchema = Frameworks.createRootSchema(true);

        // customers(id INT NOT NULL, name VARCHAR, email VARCHAR)
        Table customersTable = new AbstractTable() {
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
        };
        rootSchema.add("customers", customersTable);

        // orders(order_id INT NOT NULL, customer_id INT, amount DECIMAL, order_date DATE)
        Table ordersTable = new AbstractTable() {
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
        };
        rootSchema.add("orders", ordersTable);

        FrameworkConfig config = Frameworks.newConfigBuilder()
                .defaultSchema(rootSchema)
                .build();

        relBuilder = RelBuilder.create(config);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static Set<Symbol> setOf(Symbol... symbols) {
        return new HashSet<>(Arrays.asList(symbols));
    }

    private Constraints emptyConstraints() {
        return Constraints.build(Collections.emptyList(), setOf(), setOf());
    }

    // ── 1. testAssignTable ──────────────────────────────────────────────

    @Test
    void testAssignTable() {
        Model model = new Model(emptyConstraints());
        Symbol t0 = Symbol.of("t0");

        RelNode scan = relBuilder.scan("customers").build();

        assertTrue(model.assign(t0, scan));
        assertSame(scan, model.ofTable(t0));
        assertTrue(model.isAssigned(t0));
    }

    // ── 2. testAssignAttrs ──────────────────────────────────────────────

    @Test
    void testAssignAttrs() {
        Model model = new Model(emptyConstraints());
        Symbol a0 = Symbol.of("a0");

        List<ColumnRef> attrs = Arrays.asList(
                new ColumnRef("customers", "id"),
                new ColumnRef("customers", "name")
        );

        assertTrue(model.assign(a0, attrs));
        assertEquals(attrs, model.ofAttrs(a0));
        assertTrue(model.isAssigned(a0));
    }

    // ── 3. testAssignPred ───────────────────────────────────────────────

    @Test
    void testAssignPred() {
        Model model = new Model(emptyConstraints());
        Symbol p0 = Symbol.of("p0");

        relBuilder.scan("customers");
        RexNode pred = relBuilder.call(
                SqlStdOperatorTable.GREATER_THAN,
                relBuilder.field("id"),
                relBuilder.literal(100)
        );
        relBuilder.build(); // discard the builder state

        assertTrue(model.assign(p0, pred));
        assertSame(pred, model.ofPred(p0));
    }

    // ── 4. testDeriveAndRollback ────────────────────────────────────────

    @Test
    void testDeriveAndRollback() {
        Model parent = new Model(emptyConstraints());
        Symbol t0 = Symbol.of("t0");
        Symbol t1 = Symbol.of("t1");

        RelNode customersScan = relBuilder.scan("customers").build();
        parent.assign(t0, customersScan);

        // Derive child
        Model child = parent.derive();

        // Child sees parent's assignment
        assertSame(customersScan, child.ofTable(t0));

        // Assign in child
        relBuilder.clear();
        RelNode ordersScan = relBuilder.scan("orders").build();
        child.assign(t1, ordersScan);

        // Child sees both
        assertSame(customersScan, child.ofTable(t0));
        assertSame(ordersScan, child.ofTable(t1));

        // Parent does NOT see child's assignment
        assertNull(parent.ofTable(t1));

        // Parent's own assignment is unaffected
        assertSame(customersScan, parent.ofTable(t0));
    }

    // ── 5. testEqConsistencyPass ────────────────────────────────────────

    @Test
    void testEqConsistencyPass() {
        Symbol t0 = Symbol.of("t0");
        Symbol t1 = Symbol.of("t1");

        Constraint eq = Constraint.of(ConstraintKind.TABLE_EQ, t0, t1);
        Constraints constraints = Constraints.build(
                Arrays.asList(eq),
                setOf(t0, t1),
                setOf()
        );

        Model model = new Model(constraints);

        // Assign the same table scan to t0 and t1
        RelNode scan1 = relBuilder.scan("customers").build();
        relBuilder.clear();
        RelNode scan2 = relBuilder.scan("customers").build();

        assertTrue(model.assign(t0, scan1));
        assertTrue(model.assign(t1, scan2),
                "Assigning same-table scans to equivalent symbols should pass");
    }

    // ── 6. testEqConsistencyFail ────────────────────────────────────────

    @Test
    void testEqConsistencyFail() {
        Symbol t0 = Symbol.of("t0");
        Symbol t1 = Symbol.of("t1");

        Constraint eq = Constraint.of(ConstraintKind.TABLE_EQ, t0, t1);
        Constraints constraints = Constraints.build(
                Arrays.asList(eq),
                setOf(t0, t1),
                setOf()
        );

        Model model = new Model(constraints);

        RelNode customersScan = relBuilder.scan("customers").build();
        relBuilder.clear();
        RelNode ordersScan = relBuilder.scan("orders").build();

        assertTrue(model.assign(t0, customersScan));
        assertFalse(model.assign(t1, ordersScan),
                "Assigning different table scans to equivalent symbols should fail");
    }

    // ── 7. testCheckAttrsSub ────────────────────────────────────────────

    @Test
    void testCheckAttrsSub() {
        Symbol a0 = Symbol.of("a0");
        Symbol t0 = Symbol.of("t0");

        Constraint attrsSub = Constraint.of(ConstraintKind.ATTRS_SUB, a0, t0);
        Constraints constraints = Constraints.build(
                Arrays.asList(attrsSub),
                setOf(a0, t0),
                setOf()
        );

        Model model = new Model(constraints);

        RelNode scan = relBuilder.scan("customers").build();
        model.assign(t0, scan);

        // Assign attrs that are a valid subset of customers columns
        List<ColumnRef> validAttrs = Arrays.asList(
                new ColumnRef("customers", "id"),
                new ColumnRef("customers", "name")
        );
        model.assign(a0, validAttrs);

        assertTrue(model.checkConstraints(),
                "Attrs that are a subset of table columns should pass ATTRS_SUB");
    }

    @Test
    void testCheckAttrsSubFail() {
        Symbol a0 = Symbol.of("a0");
        Symbol t0 = Symbol.of("t0");

        Constraint attrsSub = Constraint.of(ConstraintKind.ATTRS_SUB, a0, t0);
        Constraints constraints = Constraints.build(
                Arrays.asList(attrsSub),
                setOf(a0, t0),
                setOf()
        );

        Model model = new Model(constraints);

        RelNode scan = relBuilder.scan("customers").build();
        model.assign(t0, scan);

        // Assign attrs with a column that doesn't exist in customers
        List<ColumnRef> invalidAttrs = Arrays.asList(
                new ColumnRef("customers", "nonexistent_column")
        );
        model.assign(a0, invalidAttrs);

        assertFalse(model.checkConstraints(),
                "Attrs with non-existent column should fail ATTRS_SUB");
    }

    // ── 8. testCheckNotNull ─────────────────────────────────────────────

    @Test
    void testCheckNotNull() {
        Symbol t0 = Symbol.of("t0");
        Symbol a0 = Symbol.of("a0");

        Constraint notNull = Constraint.of(ConstraintKind.NOT_NULL, t0, a0);
        Constraints constraints = Constraints.build(
                Arrays.asList(notNull),
                setOf(t0, a0),
                setOf()
        );

        Model model = new Model(constraints);

        // customers.id is NOT NULL (defined in setup)
        RelNode scan = relBuilder.scan("customers").build();
        model.assign(t0, scan);

        List<ColumnRef> notNullAttrs = Collections.singletonList(
                new ColumnRef("customers", "id")
        );
        model.assign(a0, notNullAttrs);

        assertTrue(model.checkConstraints(),
                "Non-nullable column (id) should pass NOT_NULL constraint");
    }

    @Test
    void testCheckNotNullFail() {
        Symbol t0 = Symbol.of("t0");
        Symbol a0 = Symbol.of("a0");

        Constraint notNull = Constraint.of(ConstraintKind.NOT_NULL, t0, a0);
        Constraints constraints = Constraints.build(
                Arrays.asList(notNull),
                setOf(t0, a0),
                setOf()
        );

        Model model = new Model(constraints);

        // customers.name IS nullable
        RelNode scan = relBuilder.scan("customers").build();
        model.assign(t0, scan);

        List<ColumnRef> nullableAttrs = Collections.singletonList(
                new ColumnRef("customers", "name")
        );
        model.assign(a0, nullableAttrs);

        assertFalse(model.checkConstraints(),
                "Nullable column (name) should fail NOT_NULL constraint");
    }
}
