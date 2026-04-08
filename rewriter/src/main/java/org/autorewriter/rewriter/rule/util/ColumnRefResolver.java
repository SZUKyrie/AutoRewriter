package org.autorewriter.rewriter.rule.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.ChainedRelMetadataProvider;
import org.apache.calcite.rel.metadata.DefaultRelMetadataProvider;
import org.apache.calcite.rel.metadata.JaninoRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelColumnOrigin;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.RelMdColumnOriginsInSubFilter;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.RelMdColumnOriginsForFilter;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.RelMdColumnOriginsForProject;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.RelMdColumnOriginsForJoin;
import com.google.common.collect.ImmutableList;
import org.apache.calcite.rel.type.RelDataTypeField;

import java.util.List;
import java.util.Set;

/**
 * Resolves column references between positional indices and stable ColumnRef metadata.
 */
@Slf4j
public final class ColumnRefResolver {

    private static final JaninoRelMetadataProvider METADATA_PROVIDER =
            JaninoRelMetadataProvider.of(
                    ChainedRelMetadataProvider.of(ImmutableList.of(
                            RelMdColumnOriginsForProject.SOURCE,
                            RelMdColumnOriginsForFilter.SOURCE,
                            RelMdColumnOriginsForJoin.SOURCE,
                            RelMdColumnOriginsInSubFilter.SOURCE,
                            DefaultRelMetadataProvider.INSTANCE
                    )));

    private ColumnRefResolver() {}

    private static RelMetadataQuery createMetadataQuery() {
        return new RelMetadataQuery(METADATA_PROVIDER);
    }

    /**
     * Resolve a column index within the given operator to a stable ColumnRef
     * using Calcite's RelMetadataQuery.getColumnOrigins().
     */
    public static ColumnRef resolve(int fieldIndex, RelNode operator) {
        RelNode unwrapped = unwrap(operator);

        // Validate field index is within bounds
        int fieldCount = unwrapped.getRowType().getFieldCount();
        if (fieldIndex < 0 || fieldIndex >= fieldCount) {
            throw new IllegalStateException(
                    String.format("Field index %d out of bounds for %s (has %d fields)",
                            fieldIndex, unwrapped.getRelTypeName(), fieldCount));
        }

        RelMetadataQuery mq = createMetadataQuery();
        Set<RelColumnOrigin> origins = mq.getColumnOrigins(unwrapped, fieldIndex);
        if (origins != null && !origins.isEmpty()) {
            RelColumnOrigin origin = origins.iterator().next();
            if (!origin.isDerived()) {
                RelOptTable table = origin.getOriginTable();
                String tableName = String.join(".", table.getQualifiedName());
                int originCol = origin.getOriginColumnOrdinal();
                String columnName = table.getRowType().getFieldNames().get(originCol);

                // Self-join disambiguation: when the same table.column appears at
                // multiple positions in the operator (e.g., contacts.user_id from
                // two different contacts scans in a self-join), use the operator's
                // field name to disambiguate. Calcite already appends numeric
                // suffixes (user_id vs user_id0) to disambiguate field names.
                String operatorFieldName = unwrapped.getRowType().getFieldNames().get(fieldIndex);
                if (!operatorFieldName.equalsIgnoreCase(columnName)) {
                    // Field name differs from origin column name (e.g., "user_id0" vs "user_id"),
                    // indicating a self-join duplicate. Append positional tag to table name
                    // to create a unique identity, consistent with ColumnRefRegistry.computeJoin().
                    tableName = tableName + "$" + fieldIndex;
                }

                return new ColumnRef(tableName, columnName);
            }
        }

        log.error("Cannot resolve column origin for field {} on {}: origins={}, rowType fields={}",
                fieldIndex, unwrapped.getRelTypeName(), origins,
                unwrapped.getRowType().getFieldCount());
        throw new IllegalStateException(
                String.format("Cannot resolve column origin for field index %d on %s",
                        fieldIndex, unwrapped.getRelTypeName()));
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
            RelMetadataQuery mq = createMetadataQuery();
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
        while (node instanceof HepRelVertex || node instanceof RelSubset) {
            if (node instanceof HepRelVertex) {
                node = ((HepRelVertex) node).getCurrentRel();
            } else {
                RelNode best = ((RelSubset) node).getBest();
                if (best != null) {
                    node = best;
                } else {
                    RelNode orig = ((RelSubset) node).getOriginal();
                    node = (orig != null) ? orig : node;
                    break;
                }
            }
        }
        return node;
    }
}
