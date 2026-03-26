package org.autorewriter.rewriter.optimize.costBaseOpt.insub;

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
 * Metadata handler that provides column origin information for {@link LogicalInSubFilter}.
 * <p>
 * Since {@link LogicalInSubFilter#deriveRowType()} returns {@code left.getRowType()},
 * all output columns originate from the left input — so we simply delegate to it.
 */
public class RelMdColumnOriginsInSubFilter
        implements MetadataHandler<BuiltInMetadata.ColumnOrigin> {

    public static final RelMetadataProvider SOURCE =
            ReflectiveRelMetadataProvider.reflectiveSource(
                    BuiltInMethod.COLUMN_ORIGIN.method,
                    new RelMdColumnOriginsInSubFilter());

    @Override
    public MetadataDef<BuiltInMetadata.ColumnOrigin> getDef() {
        return BuiltInMetadata.ColumnOrigin.DEF;
    }

    public Set<RelColumnOrigin> getColumnOrigins(
            LogicalInSubFilter rel, RelMetadataQuery mq, int iOutputColumn) {
        return mq.getColumnOrigins(rel.getLeft(), iOutputColumn);
    }
}
