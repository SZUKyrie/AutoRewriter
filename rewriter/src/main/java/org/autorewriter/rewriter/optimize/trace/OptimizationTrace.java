package org.autorewriter.rewriter.optimize.trace;

import lombok.Getter;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.AnsiSqlDialect;

import java.io.IOException;
import java.util.*;

/**
 * Holds the complete optimization trace for a single query.
 * <p>
 * After optimization finishes you can inspect:
 * <ul>
 *   <li>{@link #getSteps()} — ordered list of every rule that fired, with the
 *       matched (before) and produced (after) RelNode for that step.</li>
 *   <li>{@link #getIntermediateRelNodes()} — the produced RelNode of each step,
 *       i.e. every intermediate tree shape in order of appearance.</li>
 * </ul>
 */
@Getter
public class OptimizationTrace {

    /** Every rule fire event, in chronological order */
    private final List<RuleApplicationStep> steps = new ArrayList<>();

    /**
     * Append one rule-fire record.
     * Called by {@code RuleTraceListener} on every {@code ruleProductionSucceeded(isBefore=false)}.
     */
    public void addStep(RuleApplicationStep step) {
        steps.add(step);
    }

    /**
     * Returns the produced (intermediate / final) RelNode of each step in order.
     * The last element equals the final optimized tree returned by {@code findBestExp()}.
     */
    public List<RelNode> getIntermediateRelNodes() {
        List<RelNode> nodes = new ArrayList<>(steps.size());
        for (RuleApplicationStep step : steps) {
            nodes.add(step.getProducedRelNode());
        }
        return Collections.unmodifiableList(nodes);
    }

    /** Convenience: how many rules actually fired (changed the plan) */
    public int firedCount() {
        return steps.size();
    }

    /**
     * Human-readable summary, e.g. for logging.
     * <pre>
     * Optimization Trace (3 rules fired):
     *   [Step 1] Rule: AggregateReduceRule | LogicalAggregate => LogicalAggregate
     *   [Step 2] ...
     * </pre>
     */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Optimization Trace (").append(steps.size()).append(" rules fired):\n");
        for (RuleApplicationStep step : steps) {
            sb.append("  ").append(step).append("\n");
        }
        return sb.toString();
    }

    /**
     * Human-readable path-based summary for CBO traces.
     * <p>
     * Reconstructs optimization paths by chaining steps where one step's
     * producedRelNode is another step's matchedRelNode (by Calcite node ID).
     * Each path shows: initial node → [rule1] node → [rule2] node → ...
     * <pre>
     * === Optimization Paths (373 rule firings, 45 paths) ===
     *
     * --- Path 1 (3 steps) ---
     * LogicalJoin[inner]
     *   -> [AutoRewriteRule_0_stripped] LogicalProject
     *   -> [JdbcProjectRule] JdbcProject
     * </pre>
     */
    public String pathSummary() {
        if (steps.isEmpty()) {
            return "Optimization Trace (0 rules fired, no paths)";
        }

        // Build adjacency: producedNodeId -> list of steps that consumed it
        // (i.e., steps whose matchedRelNode has the same ID)
        Map<Integer, List<RuleApplicationStep>> producerToConsumers = new LinkedHashMap<>();
        Set<Integer> allMatchedIds = new HashSet<>();
        Set<Integer> allProducedIds = new HashSet<>();

        for (RuleApplicationStep step : steps) {
            int matchedId = step.getMatchedRelNode().getId();
            allMatchedIds.add(matchedId);
            allProducedIds.add(step.getProducedRelNode().getId());
            producerToConsumers
                    .computeIfAbsent(matchedId, k -> new ArrayList<>())
                    .add(step);
        }

        // Root steps: matchedRelNode was not produced by any prior step
        List<RuleApplicationStep> rootSteps = new ArrayList<>();
        for (RuleApplicationStep step : steps) {
            int matchedId = step.getMatchedRelNode().getId();
            if (!allProducedIds.contains(matchedId)) {
                rootSteps.add(step);
            }
        }

        // DFS to collect all paths from each root
        List<List<RuleApplicationStep>> allPaths = new ArrayList<>();
        for (RuleApplicationStep root : rootSteps) {
            List<RuleApplicationStep> currentPath = new ArrayList<>();
            collectPaths(root, producerToConsumers, currentPath, allPaths);
        }

        // Format output
        StringBuilder sb = new StringBuilder();
        sb.append("=== Optimization Paths (")
                .append(steps.size()).append(" rule firings, ")
                .append(allPaths.size()).append(" paths) ===\n");

        for (int i = 0; i < allPaths.size(); i++) {
            List<RuleApplicationStep> path = allPaths.get(i);
            sb.append("\n--- Path ").append(i + 1)
                    .append(" (").append(path.size()).append(" steps) ---\n");
            // Print the starting node
            sb.append(describeNode(path.get(0).getMatchedRelNode())).append("\n");
            // Print each step
            for (RuleApplicationStep step : path) {
                sb.append("  -> [").append(step.getRule().getClass().getSimpleName())
                        .append("] ").append(describeNode(step.getProducedRelNode()))
                        .append("\n");
            }
            // Print the final node's plan tree
            RelNode finalNode = path.get(path.size() - 1).getProducedRelNode();
            sb.append("  [Plan]\n");
            for (String line : finalNode.explain().split("\n")) {
                sb.append("    ").append(line).append("\n");
            }
            // Try to generate SQL (best-effort, may fail for subtrees)
            String sql = tryRelNodeToSql(finalNode);
            if (sql != null) {
                sb.append("  [SQL] ").append(sql).append("\n");
            }
        }

        return sb.toString();
    }

    private void collectPaths(RuleApplicationStep current,
                              Map<Integer, List<RuleApplicationStep>> producerToConsumers,
                              List<RuleApplicationStep> currentPath,
                              List<List<RuleApplicationStep>> allPaths) {
        currentPath.add(current);
        int producedId = current.getProducedRelNode().getId();
        List<RuleApplicationStep> consumers = producerToConsumers.get(producedId);

        if (consumers == null || consumers.isEmpty()) {
            // Leaf — this is a complete path
            allPaths.add(new ArrayList<>(currentPath));
        } else {
            for (RuleApplicationStep next : consumers) {
                collectPaths(next, producerToConsumers, currentPath, allPaths);
            }
        }
        currentPath.remove(currentPath.size() - 1);
    }

    private static String tryRelNodeToSql(RelNode node) {
        try {
            SqlDialect dialect = AnsiSqlDialect.DEFAULT;
            RelToSqlConverter converter = new RelToSqlConverter(dialect);
            RelToSqlConverter.Result result = converter.visitRoot(node);
            return result.asStatement().toSqlString(dialect).getSql();
        } catch (Throwable e) {
            // May fail for subtrees, physical nodes (JdbcToEnumerableConverter), etc.
            return null;
        }
    }

    private static String describeNode(RelNode node) {
        String typeName = node.getRelTypeName();
        // Add a brief description based on node type
        String detail = "";
        try {
            if (node instanceof org.apache.calcite.rel.logical.LogicalTableScan) {
                List<String> names = node.getTable().getQualifiedName();
                detail = "[" + names.get(names.size() - 1) + "]";
            } else if (node instanceof org.apache.calcite.rel.logical.LogicalFilter) {
                detail = "[" + ((org.apache.calcite.rel.logical.LogicalFilter) node)
                        .getCondition().toString() + "]";
                if (detail.length() > 50) detail = detail.substring(0, 47) + "...]";
            } else if (node instanceof org.apache.calcite.rel.logical.LogicalJoin) {
                org.apache.calcite.rel.logical.LogicalJoin join =
                        (org.apache.calcite.rel.logical.LogicalJoin) node;
                detail = "[" + join.getJoinType().lowerName + "]";
            }
        } catch (Exception ignored) {
            // best-effort description
        }
        return typeName + detail;
    }

    /**
     * Export the rewrite exploration tree as a PNG image via Graphviz.
     *
     * @param outputPath file path for the output PNG
     * @throws IOException if DOT generation or rendering fails
     */
    public void exportTreePng(String outputPath) throws IOException {
        TraceTreeVisualizer.exportToPng(this, outputPath);
    }
}

