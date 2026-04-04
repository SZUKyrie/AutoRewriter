package org.autorewriter.rewriter.analyze;

import lombok.Getter;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rel.logical.LogicalJoin;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rel.logical.LogicalTableScan;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexSubQuery;
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

    /**
     * Checks whether this rule is a no-op (identity rewrite).
     * <p>
     * Compares both tree structure AND placeholder symbol names. Rules with the
     * same tree shape but different placeholder positions (e.g., join reordering
     * rules that swap attribute bindings) are NOT no-ops.
     */
    public boolean isNoOp() {
        String sourceSignature = fullSignature(sourceRelNode);
        String targetSignature = fullSignature(targetRelNode);
        return sourceSignature.equals(targetSignature);
    }

    /**
     * Compute a full signature: pre-order traversal of node class names plus
     * placeholder symbol names at each node. This distinguishes rules that have
     * the same tree shape but different symbol bindings.
     */
    private static String fullSignature(RelNode node) {
        StringBuilder sb = new StringBuilder();
        buildFullSignature(node, sb);
        return sb.toString();
    }

    private static void buildFullSignature(RelNode node, StringBuilder sb) {
        sb.append(node.getClass().getSimpleName());

        // Append placeholder symbols specific to each node type
        if (node instanceof LogicalTableScan) {
            List<String> qn = ((LogicalTableScan) node).getTable().getQualifiedName();
            sb.append('<').append(qn.get(qn.size() - 1)).append('>');
        } else if (node instanceof LogicalProject) {
            sb.append('<');
            List<String> fields = node.getRowType().getFieldNames();
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) sb.append(' ');
                sb.append(fields.get(i));
            }
            sb.append('>');
        } else if (node instanceof LogicalFilter) {
            sb.append('<');
            appendRexSignature(((LogicalFilter) node).getCondition(), sb);
            sb.append('>');
        } else if (node instanceof LogicalJoin) {
            LogicalJoin join = (LogicalJoin) node;
            sb.append('<').append(join.getJoinType()).append(' ');
            appendRexSignature(join.getCondition(), sb);
            sb.append('>');
        }

        sb.append('(');
        List<RelNode> inputs = node.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            if (i > 0) sb.append(',');
            buildFullSignature(inputs.get(i), sb);
        }
        sb.append(')');
    }

    /**
     * Append a compact signature for a RexNode, including operator names,
     * input ref indices, and literal values.
     */
    private static void appendRexSignature(RexNode rex, StringBuilder sb) {
        if (rex instanceof RexCall) {
            RexCall call = (RexCall) rex;
            sb.append(call.getOperator().getName()).append('(');
            List<RexNode> operands = call.getOperands();
            for (int i = 0; i < operands.size(); i++) {
                if (i > 0) sb.append(',');
                appendRexSignature(operands.get(i), sb);
            }
            sb.append(')');
        } else if (rex instanceof RexInputRef) {
            sb.append('$').append(((RexInputRef) rex).getIndex());
        } else if (rex instanceof RexSubQuery) {
            RexSubQuery sq = (RexSubQuery) rex;
            sb.append(sq.getOperator().getName()).append('{');
            buildFullSignature(sq.rel, sb);
            sb.append('}');
        } else {
            sb.append(rex.toString());
        }
    }
}

