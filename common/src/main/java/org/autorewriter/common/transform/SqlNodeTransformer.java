package org.autorewriter.common.transform;

import org.apache.calcite.sql.SqlNode;

/**
 * Interface for SQL node transformation.
 * Implementations can transform SqlNode in various ways (normalization, optimization, etc.)
 */
public interface SqlNodeTransformer {

    /**
     * Transform the given SqlNode.
     *
     * @param node the SqlNode to transform
     * @return transformed SqlNode
     */
    SqlNode transform(SqlNode node);
}

