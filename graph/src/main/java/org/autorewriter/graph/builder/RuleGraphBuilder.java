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

/**
 * Incrementally builds a {@link RuleDependencyGraph} from {@link OptimizationTrace} records.
 *
 * <p>Uses the same producedRelNode.getId() → matchedRelNode.getId() adjacency logic
 * as OptimizationTrace#pathSummary() to reconstruct firing chains.
 */
@Slf4j
public class RuleGraphBuilder {

    private final Map<Integer, Integer> observationCounts = new HashMap<>();
    private final Map<Long, Integer>    edgeFireCounts    = new HashMap<>();
    private final Map<Long, Double>     edgeTotalBenefits = new HashMap<>();
    private final Map<Integer, String>  sourceSignatures  = new HashMap<>();
    private final Map<Integer, String>  targetSignatures  = new HashMap<>();

    /**
     * Process one query's OptimizationTrace and accumulate into internal counters.
     */
    public void record(OptimizationTrace trace) {
        // Step 1: collect distinct AutoRewriteRule firings in temporal order
        Map<String, RuleApplicationStep> distinctMap = new LinkedHashMap<>();
        for (RuleApplicationStep step : trace.getSteps()) {
            if (step.getRule() instanceof AutoRewriteRule) {
                distinctMap.putIfAbsent(step.getRule().toString(), step);
            }
        }
        if (distinctMap.isEmpty()) return;

        // Step 2: update node observation counts (once per distinct rule per trace)
        for (RuleApplicationStep step : distinctMap.values()) {
            AutoRewriteRule rule = (AutoRewriteRule) step.getRule();
            int ruleId = rule.getRuleId();
            observationCounts.merge(ruleId, 1, Integer::sum);
            sourceSignatures.putIfAbsent(ruleId, signatureOf(rule.getSourceTemplate()));
            targetSignatures.putIfAbsent(ruleId, signatureOf(rule.getTargetTemplate()));
        }

        // Step 3: enumerate all ordered subsets → each subset is one chain → accumulate edges
        List<RuleApplicationStep> distinctSteps = new ArrayList<>(distinctMap.values());
        List<List<RuleApplicationStep>> chains = new ArrayList<>();
        enumerateChains(distinctSteps, 0, new ArrayList<>(), chains);

        for (List<RuleApplicationStep> chain : chains) {
            for (int i = 0; i + 1 < chain.size(); i++) {
                int fromId = ((AutoRewriteRule) chain.get(i).getRule()).getRuleId();
                int toId   = ((AutoRewriteRule) chain.get(i + 1).getRule()).getRuleId();
                long key = edgeKey(fromId, toId);
                edgeFireCounts.merge(key, 1, Integer::sum);
                edgeTotalBenefits.merge(key, 0.0, Double::sum);
            }
        }
    }

    /**
     * Build and return a RuleDependencyGraph from the accumulated state.
     */
    public RuleDependencyGraph build() {
        Map<Integer, RuleNode>             nodes    = new HashMap<>();
        Map<Integer, List<DependencyEdge>> outEdges = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : observationCounts.entrySet()) {
            int ruleId = entry.getKey();
            nodes.put(ruleId, new RuleNode(
                    ruleId,
                    sourceSignatures.getOrDefault(ruleId, ""),
                    targetSignatures.getOrDefault(ruleId, ""),
                    entry.getValue()));
        }

        for (Map.Entry<Long, Integer> entry : edgeFireCounts.entrySet()) {
            long key    = entry.getKey();
            int  fromId = (int) (key >> 32);
            int  toId   = (int) (key & 0xFFFFFFFFL);
            int  count  = entry.getValue();
            double totalBenefit = edgeTotalBenefits.getOrDefault(key, 0.0);
            outEdges.computeIfAbsent(fromId, k -> new ArrayList<>())
                    .add(new DependencyEdge(fromId, toId, count, totalBenefit));
        }

        return new RuleDependencyGraph(nodes, outEdges);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void enumerateChains(List<RuleApplicationStep> steps, int start,
                                  List<RuleApplicationStep> current,
                                  List<List<RuleApplicationStep>> result) {
        for (int i = start; i < steps.size(); i++) {
            current.add(steps.get(i));
            result.add(new ArrayList<>(current));
            enumerateChains(steps, i + 1, current, result);
            current.remove(current.size() - 1);
        }
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

    private long edgeKey(int fromId, int toId) {
        return ((long) fromId << 32) | (toId & 0xFFFFFFFFL);
    }
}
