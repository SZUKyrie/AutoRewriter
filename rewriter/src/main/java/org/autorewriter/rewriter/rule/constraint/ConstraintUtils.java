package org.autorewriter.rewriter.rule.constraint;

import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.DefaultRelMetadataProvider;
import org.apache.calcite.rel.metadata.JaninoRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

/**
 * Shared utilities for constraint handlers.
 */
public final class ConstraintUtils {

    private static final JaninoRelMetadataProvider METADATA_PROVIDER =
            JaninoRelMetadataProvider.of(DefaultRelMetadataProvider.INSTANCE);

    private ConstraintUtils() {}

    /**
     * Unwrap HepRelVertex to get the real underlying RelNode.
     */
    public static RelNode resolveRelNode(Object binding) {
        if (binding instanceof HepRelVertex) {
            return ((HepRelVertex) binding).getCurrentRel();
        }
        if (binding instanceof RelNode) {
            return (RelNode) binding;
        }
        return null;
    }

    /**
     * Resolve the 0-based column index from "{attrParam}_index" binding.
     */
    public static Integer resolveColIndex(String attrParam, java.util.Map<String, Object> bindings) {
        Object idxObj = bindings.get(attrParam + "_index");
        if (idxObj instanceof Integer) {
            return (Integer) idxObj;
        }
        return null;
    }

    /**
     * Create a fresh RelMetadataQuery with all default handlers properly initialized.
     * Bypasses cluster.getMetadataQuery() which may return a cached instance
     * with broken/empty handler proxies inside HepPlanner.
     */
    public static RelMetadataQuery createMetadataQuery() {
        return new RelMetadataQuery(METADATA_PROVIDER);
    }
}

