package org.autorewriter.graph.model;

import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 对 {@link RuleDependencyGraph} 进行统计分析，输出规则重要性摘要。
 *
 * <p>衡量维度：
 * <ul>
 *   <li><b>出度（Out-Degree）</b>：该规则触发后紧接着触发了多少不同的后继规则。
 *       出度高 → 该规则是"触发源"，适合优先尝试。</li>
 *   <li><b>加权出度（Weighted Out-Degree）</b>：出边 fireCount 之和，反映该规则触发后续规则的总次数。</li>
 *   <li><b>入度（In-Degree）</b>：有多少规则的触发会使该规则紧接着触发。
 *       入度高 → 该规则是"被依赖的下游"，常被触发。</li>
 *   <li><b>加权入度（Weighted In-Degree）</b>：入边 fireCount 之和，反映被触发的总次数。</li>
 *   <li><b>观测次数（Observation Count）</b>：该规则在所有 trace 中独立出现的次数。</li>
 *   <li><b>PageRank 风格评分</b>：综合出度权重与入度权重，衡量在规则传播链中的中心性。</li>
 * </ul>
 */
public class GraphSummary {

    private final RuleDependencyGraph graph;

    public GraphSummary(RuleDependencyGraph graph) {
        this.graph = graph;
    }

    // ── 核心统计 ──────────────────────────────────────────────────────────

    public static class NodeStats implements Comparable<NodeStats> {
        public final String  nodeKey;
        public final int     ruleId;
        public final String  matchedType;   // 匹配的算子类型（去除 Logical 前缀）
        public final int     observationCount;
        public final int     outDegree;     // 不同后继规则数
        public final long    weightedOutDegree;  // 出边 fireCount 之和
        public final int     inDegree;      // 不同前驱规则数
        public final long    weightedInDegree;   // 入边 fireCount 之和
        public final double  importance;    // 综合重要性评分

        NodeStats(String nodeKey, int ruleId, String matchedType,
                  int observationCount,
                  int outDegree, long weightedOutDegree,
                  int inDegree,  long weightedInDegree,
                  double importance) {
            this.nodeKey           = nodeKey;
            this.ruleId            = ruleId;
            this.matchedType       = matchedType;
            this.observationCount  = observationCount;
            this.outDegree         = outDegree;
            this.weightedOutDegree = weightedOutDegree;
            this.inDegree          = inDegree;
            this.weightedInDegree  = weightedInDegree;
            this.importance        = importance;
        }

        @Override
        public int compareTo(NodeStats o) {
            return Double.compare(o.importance, this.importance); // 降序
        }
    }

    /** 计算所有节点的统计信息，按重要性降序排列。 */
    public List<NodeStats> compute() {
        var jg = graph.jgrapht();
        List<NodeStats> result = new ArrayList<>();

        for (String v : jg.vertexSet()) {
            RuleNode meta = graph.getNode(v);
            if (meta == null) continue;

            // 出度
            int outDeg = jg.outDegreeOf(v);
            long wOut  = jg.outgoingEdgesOf(v).stream()
                    .mapToLong(e -> (long) jg.getEdgeWeight(e))
                    .sum();

            // 入度
            int inDeg  = jg.inDegreeOf(v);
            long wIn   = jg.incomingEdgesOf(v).stream()
                    .mapToLong(e -> (long) jg.getEdgeWeight(e))
                    .sum();

            // 综合重要性：
            //   = α * weightedOutDegree + β * weightedInDegree + γ * observationCount
            // 权重：出度传播能力 0.4, 被依赖程度 0.4, 自身观测频度 0.2
            double importance = 0.4 * wOut + 0.4 * wIn + 0.2 * meta.getObservationCount();

            String sig = meta.getMatchedNodeSignature();
            if (sig.contains("-")) sig = sig.substring(0, sig.indexOf('-'));
            sig = sig.replace("Logical", "");

            result.add(new NodeStats(v, meta.getRuleId(), sig,
                    meta.getObservationCount(),
                    outDeg, wOut, inDeg, wIn, importance));
        }

        Collections.sort(result);
        return result;
    }

    // ── 格式化输出 ────────────────────────────────────────────────────────

    /**
     * 生成文字摘要报告，前 {@code topN} 条。
     *
     * @param topN 展示前 N 个最重要的规则节点
     */
    public String report(int topN) {
        List<NodeStats> stats = compute();
        if (stats.isEmpty()) return "=== RuleDependencyGraph Summary: empty graph ===\n";

        int show = Math.min(topN, stats.size());

        // 全局统计
        long totalObs = stats.stream().mapToLong(s -> s.observationCount).sum();
        double maxImportance = stats.get(0).importance;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "=== RuleDependencyGraph Summary (%d nodes, %d edges) ===\n",
                graph.nodeCount(), graph.edgeCount()));
        sb.append(String.format("    Total observations: %d  |  Showing top %d rules by importance\n\n",
                totalObs, show));

        // 表头
        sb.append(String.format("%-5s  %-8s  %-14s  %6s  %7s  %8s  %6s  %7s  %8s  %-20s\n",
                "Rank", "RuleId", "MatchedType",
                "ObsCnt",
                "OutDeg", "WtOut",
                "InDeg", "WtIn",
                "Score", "Bar"));
        sb.append("-".repeat(90)).append("\n");

        for (int i = 0; i < show; i++) {
            NodeStats s = stats.get(i);
            // 归一化进度条（20格），用 ASCII 字符避免中文宽度问题
            int barLen = maxImportance > 0 ? (int)(s.importance / maxImportance * 20) : 0;
            String bar = "#".repeat(barLen) + ".".repeat(20 - barLen);

            sb.append(String.format("%-5d  %-8d  %-14s  %6d  %7d  %8d  %6d  %7d  %8.1f  %s\n",
                    i + 1,
                    s.ruleId,
                    truncate(s.matchedType, 14),
                    s.observationCount,
                    s.outDegree,
                    s.weightedOutDegree,
                    s.inDegree,
                    s.weightedInDegree,
                    s.importance,
                    bar));
        }

        // 附加：专项排行
        sb.append("\n--- Top 5 by Weighted Out-Degree (high spread) ---\n");
        stats.stream()
                .sorted(Comparator.comparingLong((NodeStats s) -> s.weightedOutDegree).reversed())
                .limit(5)
                .forEach(s -> sb.append(String.format(
                        "  r%-6d %-14s  wOut=%d  (obs=%d)\n",
                        s.ruleId, truncate(s.matchedType, 14), s.weightedOutDegree, s.observationCount)));

        sb.append("\n--- Top 5 by Weighted In-Degree (most depended-on) ---\n");
        stats.stream()
                .sorted(Comparator.comparingLong((NodeStats s) -> s.weightedInDegree).reversed())
                .limit(5)
                .forEach(s -> sb.append(String.format(
                        "  r%-6d %-14s  wIn=%d  (obs=%d)\n",
                        s.ruleId, truncate(s.matchedType, 14), s.weightedInDegree, s.observationCount)));

        sb.append("\n--- Top 5 by Observation Count (most fired) ---\n");
        stats.stream()
                .sorted(Comparator.comparingInt((NodeStats s) -> s.observationCount).reversed())
                .limit(5)
                .forEach(s -> sb.append(String.format(
                        "  r%-6d %-14s  obs=%d\n",
                        s.ruleId, truncate(s.matchedType, 14), s.observationCount)));

        return sb.toString();
    }

    /** 默认展示 top 20。 */
    public String report() {
        return report(20);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }
}
