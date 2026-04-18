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
import org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment;
import org.autorewriter.rewriter.rule.symbol.SymbolKind;

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

    /**
     * The line number of this rule in the rule file (1-based).
     * A value of -1 indicates that the line number is unknown.
     */
    private final int ruleLineNumber;

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
        this.ruleLineNumber = -1;
    }

    public RuleAnalysisContext(
            RelNode sourceRelNode,
            RelNode targetRelNode,
            Collection<? extends ASTNode> matchConstraints,
            Collection<? extends ASTNode> rewriteConstraints,
            int ruleLineNumber) {
        this.sourceRelNode = sourceRelNode;
        this.targetRelNode = targetRelNode;
        this.matchConstraints = matchConstraints != null ?
            new ArrayList<>(matchConstraints) : Collections.emptyList();
        this.rewriteConstraints = rewriteConstraints != null ?
            new ArrayList<>(rewriteConstraints) : Collections.emptyList();
        this.ruleLineNumber = ruleLineNumber;
    }

    /**
     * Checks whether this rule is a no-op (identity rewrite).
     * <p>
     * Compares both tree structure AND placeholder symbol names, taking into
     * account equality constraints (TableEq, AttrsEq, PredicateEq, SchemaEq)
     * from both match and rewrite constraints. When constraints declare that
     * target placeholders are equivalent to source placeholders (e.g.,
     * {@code TableEq(t1,t0)}, {@code AttrsEq(a1,a0)}), the target placeholders
     * are normalized to their source equivalents before comparison.
     * <p>
     * Rules with the same tree shape but different placeholder positions that are
     * NOT linked by equality constraints (e.g., join reordering rules that swap
     * attribute bindings) are NOT no-ops.
     */
    public boolean isNoOp() {
        Map<String, String> eqMapping = buildEqualityMapping();
        String sourceSignature = normalizedSignature(sourceRelNode, eqMapping);
        String targetSignature = normalizedSignature(targetRelNode, eqMapping);
        return sourceSignature.equals(targetSignature);
    }

    /**
     * Builds a mapping from placeholder names to their canonical (source-side)
     * equivalents by extracting equality constraints (TableEq, AttrsEq,
     * PredicateEq, SchemaEq) from both match and rewrite constraints.
     * <p>
     * Uses a union-find approach: for each equality constraint, the two
     * placeholder names are merged into the same equivalence class. The
     * canonical representative is chosen as the lexicographically smallest name
     * in each class, which for typical rules (a0 vs a1, t0 vs t1) picks the
     * source-side placeholder.
     */
    private Map<String, String> buildEqualityMapping() {
        // Collect all equality pairs from constraints
        List<String[]> eqPairs = new ArrayList<>();
        collectEqualityPairs(matchConstraints, eqPairs);
        collectEqualityPairs(rewriteConstraints, eqPairs);

        if (eqPairs.isEmpty()) {
            return Collections.emptyMap();
        }

        // Build union-find for placeholder name equivalence
        Map<String, String> parent = new HashMap<>();
        for (String[] pair : eqPairs) {
            parent.putIfAbsent(pair[0], pair[0]);
            parent.putIfAbsent(pair[1], pair[1]);
            union(parent, pair[0], pair[1]);
        }

        // Map every placeholder to its canonical representative (smallest in class)
        Map<String, String> mapping = new HashMap<>();
        for (String name : parent.keySet()) {
            String root = find(parent, name);
            if (!name.equals(root)) {
                mapping.put(name, root);
            }
        }
        return mapping;
    }

    private static void collectEqualityPairs(List<ASTNode> constraints, List<String[]> pairs) {
        if (constraints == null) return;
        for (ASTNode node : constraints) {
            if (!(node instanceof ConstraintSegment)) continue;
            ConstraintSegment cs = (ConstraintSegment) node;
            String typeName = cs.getType().name();
            if ("TABLE_EQ".equals(typeName) || "ATTRS_EQ".equals(typeName)
                    || "PREDICATE_EQ".equals(typeName) || "SCHEMA_EQ".equals(typeName)) {
                String[] params = cs.getParams();
                if (params.length == 2
                        && SymbolKind.isSymbolName(params[0])
                        && SymbolKind.isSymbolName(params[1])) {
                    pairs.add(params);
                }
            }
        }
    }

    private static String find(Map<String, String> parent, String x) {
        String root = x;
        while (!root.equals(parent.get(root))) {
            root = parent.get(root);
        }
        // Path compression
        while (!x.equals(root)) {
            String next = parent.get(x);
            parent.put(x, root);
            x = next;
        }
        return root;
    }

    private static void union(Map<String, String> parent, String a, String b) {
        String ra = find(parent, a);
        String rb = find(parent, b);
        if (!ra.equals(rb)) {
            // Use lexicographically smaller name as the canonical representative
            if (ra.compareTo(rb) <= 0) {
                parent.put(rb, ra);
            } else {
                parent.put(ra, rb);
            }
        }
    }

    /**
     * Compute a normalized signature: same as fullSignature but replaces
     * placeholder names using the given equivalence mapping.
     */
    private static String normalizedSignature(RelNode node, Map<String, String> eqMapping) {
        StringBuilder sb = new StringBuilder();
        buildFullSignature(node, sb, eqMapping);
        return sb.toString();
    }

    private static void buildFullSignature(RelNode node, StringBuilder sb,
                                            Map<String, String> eqMapping) {
        sb.append(node.getClass().getSimpleName());

        // Append placeholder symbols specific to each node type
        if (node instanceof LogicalTableScan) {
            List<String> qn = ((LogicalTableScan) node).getTable().getQualifiedName();
            sb.append('<').append(normalize(qn.get(qn.size() - 1), eqMapping)).append('>');
        } else if (node instanceof LogicalProject) {
            sb.append('<');
            List<String> fields = node.getRowType().getFieldNames();
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) sb.append(' ');
                sb.append(normalize(fields.get(i), eqMapping));
            }
            sb.append('>');
        } else if (node instanceof LogicalFilter) {
            sb.append('<');
            appendRexSignature(((LogicalFilter) node).getCondition(), sb, eqMapping);
            sb.append('>');
        } else if (node instanceof LogicalJoin) {
            LogicalJoin join = (LogicalJoin) node;
            sb.append('<').append(join.getJoinType()).append(' ');
            appendRexSignature(join.getCondition(), sb, eqMapping);
            sb.append('>');
        }

        sb.append('(');
        List<RelNode> inputs = node.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            if (i > 0) sb.append(',');
            buildFullSignature(inputs.get(i), sb, eqMapping);
        }
        sb.append(')');
    }

    /**
     * Normalize a placeholder name through the equivalence mapping.
     * Non-placeholder names are returned as-is.
     */
    private static String normalize(String name, Map<String, String> eqMapping) {
        return eqMapping.getOrDefault(name, name);
    }

    /**
     * Append a compact signature for a RexNode, including operator names,
     * input ref indices, and literal values. Placeholder names in operator
     * names are normalized through the equivalence mapping.
     */
    private static void appendRexSignature(RexNode rex, StringBuilder sb,
                                            Map<String, String> eqMapping) {
        if (rex instanceof RexCall) {
            RexCall call = (RexCall) rex;
            sb.append(normalize(call.getOperator().getName(), eqMapping)).append('(');
            List<RexNode> operands = call.getOperands();
            for (int i = 0; i < operands.size(); i++) {
                if (i > 0) sb.append(',');
                appendRexSignature(operands.get(i), sb, eqMapping);
            }
            sb.append(')');
        } else if (rex instanceof RexInputRef) {
            sb.append('$').append(((RexInputRef) rex).getIndex());
        } else if (rex instanceof RexSubQuery) {
            RexSubQuery sq = (RexSubQuery) rex;
            sb.append(sq.getOperator().getName()).append('{');
            buildFullSignature(sq.rel, sb, eqMapping);
            sb.append('}');
        } else {
            sb.append(rex.toString());
        }
    }
}

