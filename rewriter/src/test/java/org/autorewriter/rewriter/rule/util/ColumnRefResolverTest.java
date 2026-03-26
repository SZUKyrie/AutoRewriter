package org.autorewriter.rewriter.rule.util;

import com.google.common.collect.ImmutableList;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.metadata.DefaultRelMetadataProvider;
import org.apache.calcite.rel.metadata.JaninoRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelColumnOrigin;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexSubQuery;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.InSubFilterExpander;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.LogicalInSubFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ColumnRefResolver}, specifically verifying that column origin
 * resolution works correctly for {@link LogicalInSubFilter} nodes via the
 * registered {@link org.autorewriter.rewriter.optimize.costBaseOpt.insub.RelMdColumnOriginsInSubFilter}
 * metadata handler.
 */
public class ColumnRefResolverTest {

    private RelBuilder relBuilder;

    @BeforeEach
    public void setup() {
        SchemaPlus rootSchema = Frameworks.createRootSchema(true);

        rootSchema.add("customers", new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return typeFactory.builder()
                        .add("id", SqlTypeName.INTEGER)
                        .add("name", SqlTypeName.VARCHAR, 100)
                        .build();
            }
        });

        rootSchema.add("orders", new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return typeFactory.builder()
                        .add("order_id", SqlTypeName.INTEGER)
                        .add("customer_id", SqlTypeName.INTEGER)
                        .build();
            }
        });

        FrameworkConfig config = Frameworks.newConfigBuilder()
                .defaultSchema(rootSchema)
                .build();
        relBuilder = RelBuilder.create(config);
    }

    /**
     * Builds: SELECT * FROM customers WHERE id IN (SELECT customer_id FROM orders)
     * and expands the filter to LogicalInSubFilter.
     */
    private LogicalInSubFilter buildInSubFilter() {
        RelNode subquery = relBuilder.scan("orders")
                .project(relBuilder.field("customer_id"))
                .build();

        relBuilder.clear();

        RelNode filter = relBuilder.scan("customers")
                .filter(RexSubQuery.in(subquery,
                        ImmutableList.of(relBuilder.field("id"))))
                .build();

        RelNode expanded = InSubFilterExpander.expand(filter);
        assertInstanceOf(LogicalInSubFilter.class, expanded,
                "InSubFilterExpander should produce LogicalInSubFilter");
        return (LogicalInSubFilter) expanded;
    }

    // -------------------------------------------------------------------------
    // Regression test: documents original bug with DefaultRelMetadataProvider
    // -------------------------------------------------------------------------

    /**
     * Verifies that Calcite's default metadata provider has NO handler for
     * LogicalInSubFilter — i.e., this is the original bug being fixed.
     */
    @Test
    public void testDefaultMetadataProviderReturnsNullForLogicalInSubFilter() {
        LogicalInSubFilter inSubFilter = buildInSubFilter();

        JaninoRelMetadataProvider defaultOnly =
                JaninoRelMetadataProvider.of(DefaultRelMetadataProvider.INSTANCE);
        RelMetadataQuery mq = new RelMetadataQuery(defaultOnly);

        Set<RelColumnOrigin> origins = mq.getColumnOrigins(inSubFilter, 0);
        assertNull(origins,
                "DefaultRelMetadataProvider should return null for LogicalInSubFilter "
                        + "(no handler registered) — this is the original bug");
    }

    // -------------------------------------------------------------------------
    // Fix verification: ColumnRefResolver must resolve through LogicalInSubFilter
    // -------------------------------------------------------------------------

    /**
     * resolve(0, logicalInSubFilter) must return customers.id, not $unknown.id.
     */
    @Test
    public void testResolveColumnOriginThroughLogicalInSubFilter() {
        LogicalInSubFilter inSubFilter = buildInSubFilter();

        ColumnRef ref = ColumnRefResolver.resolve(0, inSubFilter);

        System.out.println("Resolved column ref: " + ref);
        assertNotEquals("$unknown", ref.getTableName(),
                "Column origin should be resolved to actual table, not '$unknown'");
        assertEquals("customers", ref.getTableName());
        assertEquals("id", ref.getColumnName());
    }

    /**
     * resolve(1, logicalInSubFilter) must return customers.name.
     */
    @Test
    public void testResolveSecondColumnThroughLogicalInSubFilter() {
        LogicalInSubFilter inSubFilter = buildInSubFilter();

        ColumnRef ref = ColumnRefResolver.resolve(1, inSubFilter);

        System.out.println("Resolved column ref: " + ref);
        assertNotEquals("$unknown", ref.getTableName());
        assertEquals("customers", ref.getTableName());
        assertEquals("name", ref.getColumnName());
    }

    /**
     * resolveIndex must be the inverse of resolve: given a ColumnRef for
     * customers.id, it should return index 0 on the LogicalInSubFilter.
     */
    @Test
    public void testResolveIndexThroughLogicalInSubFilter() {
        LogicalInSubFilter inSubFilter = buildInSubFilter();

        ColumnRef ref = new ColumnRef("customers", "id");
        int index = ColumnRefResolver.resolveIndex(ref, inSubFilter);

        assertEquals(0, index,
                "resolveIndex should find customers.id at position 0");
    }

    /**
     * Verifies resolve + resolveIndex are inverses across all output columns.
     */
    @Test
    public void testResolveAndResolveIndexAreInverses() {
        LogicalInSubFilter inSubFilter = buildInSubFilter();
        int fieldCount = inSubFilter.getRowType().getFieldCount();

        for (int i = 0; i < fieldCount; i++) {
            ColumnRef ref = ColumnRefResolver.resolve(i, inSubFilter);
            assertNotEquals("$unknown", ref.getTableName(),
                    "Field " + i + " should resolve to a known table");

            int roundTrip = ColumnRefResolver.resolveIndex(ref, inSubFilter);
            assertEquals(i, roundTrip,
                    "resolveIndex(resolve(i)) should equal i for field " + i);
        }
    }

    // -------------------------------------------------------------------------
    // Sanity: plain TableScan still works after provider change
    // -------------------------------------------------------------------------

    /**
     * Ensure the ChainedRelMetadataProvider change didn't break resolution
     * for ordinary TableScan nodes.
     */
    @Test
    public void testResolveOnTableScanStillWorks() {
        RelNode scan = relBuilder.scan("customers").build();

        ColumnRef ref0 = ColumnRefResolver.resolve(0, scan);
        ColumnRef ref1 = ColumnRefResolver.resolve(1, scan);

        assertEquals(new ColumnRef("customers", "id"), ref0);
        assertEquals(new ColumnRef("customers", "name"), ref1);
    }

    /**
     * Nested: LogicalInSubFilter whose left input is another LogicalInSubFilter.
     * Resolution should still propagate correctly.
     */
    @Test
    public void testResolveOnNestedLogicalInSubFilter() {
        // Inner: customers WHERE id IN (SELECT customer_id FROM orders)
        LogicalInSubFilter inner = buildInSubFilter();

        // Outer: wrap inner with another IN subquery on the same subquery
        RelNode subquery2 = relBuilder.scan("orders")
                .project(relBuilder.field("customer_id"))
                .build();
        relBuilder.clear();

        LogicalInSubFilter outer = LogicalInSubFilter.create(
                inner,
                subquery2,
                inner.getCluster().getRexBuilder().makeInputRef(
                        inner.getRowType().getFieldList().get(0).getType(), 0));

        ColumnRef ref = ColumnRefResolver.resolve(0, outer);
        System.out.println("Nested resolved: " + ref);
        assertNotEquals("$unknown", ref.getTableName(),
                "Nested LogicalInSubFilter should still resolve to a known table");
        assertEquals("customers", ref.getTableName());
        assertEquals("id", ref.getColumnName());
    }
}
