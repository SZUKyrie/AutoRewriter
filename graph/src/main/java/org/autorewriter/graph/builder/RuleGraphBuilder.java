package org.autorewriter.graph.builder;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.autorewriter.graph.model.DependencyEdge;
import org.autorewriter.graph.model.RuleDependencyGraph;
import org.autorewriter.graph.model.RuleNode;
import org.autorewriter.rewriter.optimize.trace.OptimizationTrace;
import org.autorewriter.rewriter.optimize.trace.RuleApplicationStep;
import org.autorewriter.rewriter.rule.AutoRewriteRule;

import java.util.*;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Incrementally builds a {@link RuleDependencyGraph} from {@link OptimizationTrace} records.
 *
 * <p>Graph nodes are keyed by {@code "ruleId:matchedNodeSignature"}, so the same rule
 * fired at different query sub-plan positions appears as distinct nodes.
 *
 * <p>Edges are built using RelNode ID linkage:
 * {@code stepA.producedRelNode.getId() == stepB.matchedRelNode.getId()}
 * means stepA → stepB (A's output was consumed by B).
 * This mirrors the adjacency logic in {@link OptimizationTrace#pathSummary()}.
 */
@Slf4j
public class RuleGraphBuilder {

    /** nodeKey → accumulated observation count */
    private final Map<String, Integer> observationCounts = new HashMap<>();

    /** (fromNodeKey, toNodeKey) → accumulated fire count */
    private final Map<String, Integer> edgeFireCounts = new HashMap<>();

    /** (fromNodeKey, toNodeKey) → accumulated total benefit */
    private final Map<String, Double> edgeTotalBenefits = new HashMap<>();

    /** nodeKey → RuleNode metadata (populated on first observation) */
    private final Map<String, RuleNode> nodeMetadata = new HashMap<>();

    public void record(OptimizationTrace trace) {
        List<RuleApplicationStep> autoSteps = new ArrayList<>();
        for (RuleApplicationStep step : trace.getSteps()) {
            if (step.getRule() instanceof AutoRewriteRule) {
                autoSteps.add(step);
            }
        }
        if (autoSteps.isEmpty()) return;

        // Update node observation counts and rank for every step
        for (int i = 0; i < autoSteps.size(); i++) {
            RuleApplicationStep step = autoSteps.get(i);
            String nodeKey = nodeKeyOf(step);
            observationCounts.merge(nodeKey, 1, Integer::sum);
            if (!nodeMetadata.containsKey(nodeKey)) {
                nodeMetadata.put(nodeKey, buildRuleNode(step, nodeKey));
            }
            // Update rank: use minimum position across all traces
            int currentRank = nodeMetadata.get(nodeKey).getRank();
            if (currentRank < 0 || i < currentRank) {
                nodeMetadata.get(nodeKey).setRank(i);
            }
        }

        // VolcanoPlanner applies rules in parallel (no strict causal chain).
        // We model co-occurrence as a directed graph preserving temporal order:
        // if rule A appears before rule B in the trace step sequence, we add A→B only.
        //
        // Two rules at the same matchedId (same sub-plan position) are direct
        // alternatives; we still use temporal order to decide direction.
        //
        // Deduplication: use distinct (nodeKey) list so that a rule firing multiple
        // times at the same position is counted only once per trace.

        // Deduplicated ordered list of node keys (preserving first-occurrence order)
        List<String> distinctKeys = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (RuleApplicationStep step : autoSteps) {
            String key = nodeKeyOf(step);
            if (seen.add(key)) {
                distinctKeys.add(key);
            }
        }

        // For every ordered pair (i < j), add a directed edge i → j only.
        // This preserves all co-occurrence relationships between rules.
        for (int i = 0; i < distinctKeys.size(); i++) {
            for (int j = i + 1; j < distinctKeys.size(); j++) {
                String fromKey = distinctKeys.get(i);
                String toKey   = distinctKeys.get(j);
                if (!fromKey.equals(toKey)) {
                    addEdge(fromKey, toKey);
                }
            }
        }
    }

    private void addEdge(String fromKey, String toKey) {
        String edgeKey = fromKey + "->" + toKey;
        edgeFireCounts.merge(edgeKey, 1, Integer::sum);
        edgeTotalBenefits.merge(edgeKey, 0.0, Double::sum);
    }

    public RuleDependencyGraph build() {
        Map<String, RuleNode> nodes = new HashMap<>(nodeMetadata);
        // Update observation counts from accumulated state
        for (Map.Entry<String, Integer> entry : observationCounts.entrySet()) {
            RuleNode existing = nodes.get(entry.getKey());
            if (existing != null) {
                RuleNode updated = new RuleNode(
                        existing.getNodeKey(),
                        existing.getRuleId(),
                        existing.getSourceTemplateSignature(),
                        existing.getTargetTemplateSignature(),
                        existing.getMatchedNodeSignature(),
                        entry.getValue());
                updated.setRank(existing.getRank());  // preserve rank
                nodes.put(entry.getKey(), updated);
            }
        }

        Map<String, List<DependencyEdge>> outEdges = new HashMap<>();
        for (Map.Entry<String, Integer> entry : edgeFireCounts.entrySet()) {
            String edgeKey = entry.getKey();
            int arrowIdx = edgeKey.indexOf("->");
            String fromKey = edgeKey.substring(0, arrowIdx);
            String toKey   = edgeKey.substring(arrowIdx + 2);
            int count = entry.getValue();
            double totalBenefit = edgeTotalBenefits.getOrDefault(edgeKey, 0.0);
            outEdges.computeIfAbsent(fromKey, k -> new ArrayList<>())
                    .add(new DependencyEdge(fromKey, toKey, count, totalBenefit));
        }

        return new RuleDependencyGraph(nodes, outEdges);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Node key = "ruleId:matchedNodeSignature" */
    private String nodeKeyOf(RuleApplicationStep step) {
        AutoRewriteRule rule = (AutoRewriteRule) step.getRule();
        String matchedSig = signatureOf(step.getMatchedRelNode());
        return RuleNode.keyOf(rule.getRuleId(), matchedSig);
    }

    private RuleNode buildRuleNode(RuleApplicationStep step, String nodeKey) {
        AutoRewriteRule rule = (AutoRewriteRule) step.getRule();
        return new RuleNode(
                nodeKey,
                rule.getRuleId(),
                signatureOf(rule.getSourceTemplate()),
                signatureOf(rule.getTargetTemplate()),
                signatureOf(step.getMatchedRelNode()),
                0);
    }

    private String signatureOf(RelNode node) {
        if (node == null) return "";
        StringBuilder sb = new StringBuilder();
        appendSignature(node, sb);
        return sb.toString();
    }

    private void appendSignature(RelNode node, StringBuilder sb) {
        if (sb.length() > 0) sb.append('-');
        sb.append(node.getRelTypeName());
        for (RelNode child : node.getInputs()) {
            appendSignature(child, sb);
        }
    }
}

