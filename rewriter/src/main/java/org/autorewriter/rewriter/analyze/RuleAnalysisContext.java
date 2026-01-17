package org.autorewriter.rewriter.analyze;

import lombok.Getter;
import org.apache.calcite.rel.RelNode;
import org.apache.shardingsphere.sql.parser.api.ASTNode;

import java.util.*;

/**
 * Context containing the analysis result of a rewrite rule.
 * Includes source and target RelNodes, match constraints, and rewrite constraints.
 */
@Getter
public class RuleAnalysisContext {

    private final RelNode sourceRelNode;
    private final RelNode targetRelNode;

    /**
     * Match constraints: used during matching phase to validate bindings.
     * Parameters come from source template only.
     */
    private final List<ASTNode> matchConstraints;

    /**
     * Rewrite constraints: used during rewrite phase to transform bindings.
     * Parameters can come from both source and target templates.
     */
    private final List<ASTNode> rewriteConstraints;

    public RuleAnalysisContext(
            RelNode sourceRelNode,
            RelNode targetRelNode,
            Collection<? extends ASTNode> matchConstraints,
            Collection<? extends ASTNode> rewriteConstraints) {
        this.sourceRelNode = sourceRelNode;
        this.targetRelNode = targetRelNode;
        this.matchConstraints = matchConstraints != null ?
            new ArrayList<>(matchConstraints) : Collections.emptyList();
        this.rewriteConstraints = rewriteConstraints != null ?
            new ArrayList<>(rewriteConstraints) : Collections.emptyList();
    }
}

