package org.autorewriter.rewriter.optimize.costBaseOpt.insub;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
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
 * Metadata handler for Join column origins.
 * Delegates to left or right input based on field index.
 */
@Slf4j
public class RelMdColumnOriginsForJoin
        implements MetadataHandler<BuiltInMetadata.ColumnOrigin> {

    public static final RelMetadataProvider SOURCE =
            ReflectiveRelMetadataProvider.reflectiveSource(
                    BuiltInMethod.COLUMN_ORIGIN.method,
                    new RelMdColumnOriginsForJoin());

    @Override
    public MetadataDef<BuiltInMetadata.ColumnOrigin> getDef() {
        return BuiltInMetadata.ColumnOrigin.DEF;
    }

    public Set<RelColumnOrigin> getColumnOrigins(
            Join rel, RelMetadataQuery mq, int iOutputColumn) {
        int leftFieldCount = rel.getLeft().getRowType().getFieldCount();

        if (iOutputColumn < leftFieldCount) {
            RelNode left = unwrap(rel.getLeft());
            return mq.getColumnOrigins(left, iOutputColumn);
        } else {
            RelNode right = unwrap(rel.getRight());
            return mq.getColumnOrigins(right, iOutputColumn - leftFieldCount);
        }
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
