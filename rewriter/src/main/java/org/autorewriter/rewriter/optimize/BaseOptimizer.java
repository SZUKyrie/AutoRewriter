package org.autorewriter.rewriter.optimize;

import org.apache.calcite.rel.RelNode;

public interface BaseOptimizer {
    public RelNode optimize(RelNode root);
}
