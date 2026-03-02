package org.autorewriter.rewriter.rule.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelColumnOrigin;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.autorewriter.rewriter.rule.constraint.ConstraintUtils;

import java.util.List;
import java.util.Set;

/**
 * Resolves column references between positional indices and stable ColumnRef metadata.
 */
@Slf4j
public final class ColumnRefResolver {

    private ColumnRefResolver() {}

    /**
     * Resolve a column index within the given operator to a stable ColumnRef
     * using Calcite's RelMetadataQuery.getColumnOrigins().
     */
    public static ColumnRef resolve(int fieldIndex, RelNode operator) {
        operator = unwrap(operator);

        try {
            RelMetadataQuery mq = ConstraintUtils.createMetadataQuery();
            Set<RelColumnOrigin> origins = mq.getColumnOrigins(operator, fieldIndex);

            if (origins != null && !origins.isEmpty()) {
                RelColumnOrigin origin = origins.iterator().next();
                if (!origin.isDerived()) {
                    RelOptTable table = origin.getOriginTable();
                    String tableName = String.join(".", table.getQualifiedName());
                    int originCol = origin.getOriginColumnOrdinal();
                    String columnName = table.getRowType().getFieldNames().get(originCol);
                    return new ColumnRef(tableName, columnName);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to resolve column origin via metadata for index {} on {}: {}",
                    fieldIndex, operator.getRelTypeName(), e.getMessage());
        }

        String columnName = operator.getRowType().getFieldNames().get(fieldIndex);
        return new ColumnRef("$unknown", columnName);
    }

    /**
     * Resolve a ColumnRef back to a positional index within the given operator's row type
     * using Calcite's RelMetadataQuery.getColumnOrigins().
     * Returns -1 if not found.
     */
    public static int resolveIndex(ColumnRef ref, RelNode operator) {
        operator = unwrap(operator);
        List<RelDataTypeField> fields = operator.getRowType().getFieldList();

        try {
            RelMetadataQuery mq = ConstraintUtils.createMetadataQuery();
            for (int i = 0; i < fields.size(); i++) {
                Set<RelColumnOrigin> origins = mq.getColumnOrigins(operator, i);
                if (origins != null && !origins.isEmpty()) {
                    RelColumnOrigin origin = origins.iterator().next();
                    if (!origin.isDerived()) {
                        RelOptTable table = origin.getOriginTable();
                        String tableName = String.join(".", table.getQualifiedName());
                        int originCol = origin.getOriginColumnOrdinal();
                        String columnName = table.getRowType().getFieldNames().get(originCol);
                        if (ref.equals(new ColumnRef(tableName, columnName))) {
                            return i;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to resolve index via metadata for {} on {}: {}",
                    ref, operator.getRelTypeName(), e.getMessage());
        }

        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getName().equalsIgnoreCase(ref.getColumnName())) {
                return i;
            }
        }
        return -1;
    }

    private static RelNode unwrap(RelNode node) {
        if (node instanceof HepRelVertex) {
            return ((HepRelVertex) node).getCurrentRel();
        }
        return node;
    }
}
