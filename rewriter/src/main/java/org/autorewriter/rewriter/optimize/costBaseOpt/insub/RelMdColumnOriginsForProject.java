package org.autorewriter.rewriter.optimize.costBaseOpt.insub;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.metadata.BuiltInMetadata;
import org.apache.calcite.rel.metadata.MetadataDef;
import org.apache.calcite.rel.metadata.MetadataHandler;
import org.apache.calcite.rel.metadata.ReflectiveRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelColumnOrigin;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.util.BuiltInMethod;

import java.util.HashSet;
import java.util.Set;

/**
 * Metadata handler for Project column origins.
 */
@Slf4j
public class RelMdColumnOriginsForProject
        implements MetadataHandler<BuiltInMetadata.ColumnOrigin> {

    public static final RelMetadataProvider SOURCE =
            ReflectiveRelMetadataProvider.reflectiveSource(
                    BuiltInMethod.COLUMN_ORIGIN.method,
                    new RelMdColumnOriginsForProject());

    @Override
    public MetadataDef<BuiltInMetadata.ColumnOrigin> getDef() {
        return BuiltInMetadata.ColumnOrigin.DEF;
    }

    public Set<RelColumnOrigin> getColumnOrigins(
            Project rel, RelMetadataQuery mq, int iOutputColumn) {
        RexNode expr = rel.getProjects().get(iOutputColumn);
        if (expr instanceof RexInputRef) {
            int inputIndex = ((RexInputRef) expr).getIndex();
            RelNode input = unwrap(rel.getInput());
            return mq.getColumnOrigins(input, inputIndex);
        }
        return new HashSet<>();
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
