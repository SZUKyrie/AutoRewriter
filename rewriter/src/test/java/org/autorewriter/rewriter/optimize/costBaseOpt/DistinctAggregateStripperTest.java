package org.autorewriter.rewriter.optimize.costBaseOpt;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalAggregate;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DistinctAggregateStripper}.
 */
public class DistinctAggregateStripperTest {

    private RelBuilder relBuilder;

    @BeforeEach
    public void setup() {
        SchemaPlus rootSchema = Frameworks.createRootSchema(true);

        Table t0 = new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                return typeFactory.builder()
                        .add("a0", SqlTypeName.INTEGER)
                        .add("a1", SqlTypeName.VARCHAR, 100)
                        .build();
            }
        };
        rootSchema.add("t0", t0);

        FrameworkConfig config = Frameworks.newConfigBuilder()
                .defaultSchema(rootSchema)
                .build();
        relBuilder = RelBuilder.create(config);
    }

    @Test
    public void testIsDistinctAggregate_true() {
        // scan("t0").project(field("a0")).distinct() produces LogicalAggregate with empty aggCallList
        RelNode node = relBuilder
                .scan("t0")
                .project(relBuilder.field("a0"))
                .distinct()
                .build();

        assertTrue(node instanceof LogicalAggregate,
                "Expected LogicalAggregate but got " + node.getClass().getSimpleName());
        assertTrue(DistinctAggregateStripper.isDistinctAggregate(node),
                "Expected isDistinctAggregate to return true for DISTINCT aggregate");
    }

    @Test
    public void testIsDistinctAggregate_falseForProject() {
        // A plain project is not a distinct aggregate
        RelNode node = relBuilder
                .scan("t0")
                .project(relBuilder.field("a0"))
                .build();

        assertFalse(DistinctAggregateStripper.isDistinctAggregate(node),
                "Expected isDistinctAggregate to return false for LogicalProject");
    }

    @Test
    public void testStrip_returnsChild() {
        // Build: scan -> project -> distinct (aggregate)
        RelNode node = relBuilder
                .scan("t0")
                .project(relBuilder.field("a0"))
                .distinct()
                .build();

        RelNode stripped = DistinctAggregateStripper.strip(node);

        assertNotNull(stripped, "strip() should return the child of a DISTINCT aggregate");
        assertTrue(stripped instanceof LogicalProject,
                "Expected LogicalProject but got " + stripped.getClass().getSimpleName());
    }

    @Test
    public void testStrip_returnsNullForNonAggregate() {
        // A plain project -- strip should return null
        RelNode node = relBuilder
                .scan("t0")
                .project(relBuilder.field("a0"))
                .build();

        RelNode stripped = DistinctAggregateStripper.strip(node);

        assertNull(stripped, "strip() should return null for a non-DISTINCT-aggregate node");
    }
}
