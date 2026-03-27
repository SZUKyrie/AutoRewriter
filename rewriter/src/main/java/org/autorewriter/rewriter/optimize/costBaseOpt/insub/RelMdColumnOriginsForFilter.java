package org.autorewriter.rewriter.optimize.costBaseOpt.insub;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.metadata.BuiltInMetadata;
import org.apache.calcite.rel.metadata.MetadataDef;
import org.apache.calcite.rel.metadata.MetadataHandler;
import org.apache.calcite.rel.metadata.ReflectiveRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelColumnOrigin;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.util.BuiltInMethod;

import java.util.Set;

/**
 * Metadata handler for Filter column origins.
 * Delegates to the input since Filter doesn't transform columns.
 */
@Slf4j
public class RelMdColumnOriginsForFilter
        implements MetadataHandler<BuiltInMetadata.ColumnOrigin> {

    public static final RelMetadataProvider SOURCE =
            ReflectiveRelMetadataProvider.reflectiveSource(
                    BuiltInMethod.COLUMN_ORIGIN.method,
                    new RelMdColumnOriginsForFilter());

    @Override
    public MetadataDef<BuiltInMetadata.ColumnOrigin> getDef() {
        return BuiltInMetadata.ColumnOrigin.DEF;
    }

    public Set<RelColumnOrigin> getColumnOrigins(
            Filter rel, RelMetadataQuery mq, int iOutputColumn) {
        RelNode input = unwrap(rel.getInput());

        // Validate field index is within bounds
        if (iOutputColumn < 0 || iOutputColumn >= rel.getRowType().getFieldCount()) {
            log.info("Field index  out of bounds for Filter (has {} fields)",
                    iOutputColumn, rel.getRowType().getFieldCount());
            return null;
        }

        Set<RelColumnOrigin> origins = mq.getColumnOrigins(input, iOutputColumn);
        if (origins == null || origins.isEmpty()) {
            log.info("Filter field {}: no origins from input {} (fieldCount={})",
                    iOutputColumn, input.getRelTypeName(), input.getRowType().getFieldCount());
        }
        return origins;
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
