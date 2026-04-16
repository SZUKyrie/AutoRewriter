package org.autorewriter.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory rule dependency graph backed by JGraphT.
 *
 * <p>节点键：{@code "ruleId:matchedNodeSignature"}，边权重：fireCount。
 *
 * <p>公开接口与旧版保持兼容（{@link #getNodes()}、{@link #getOutEdges()}），
 * 同时通过 {@link #jgrapht()} 暴露完整 JGraphT 图，支持任意算法。
 */
public class RuleDependencyGraph {

    /** JGraphT 底层图，边权重 = fireCount */
    private final DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> jg;

    /** nodeKey → RuleNode 元数据 */
    private final Map<String, RuleNode> nodes;

    /** nodeKey → 出边列表（与 jg 保持同步，供旧接口使用） */
    private final Map<String, List<DependencyEdge>> outEdges;

    @JsonCreator
    public RuleDependencyGraph(
            @JsonProperty("nodes")    Map<String, RuleNode>            nodes,
            @JsonProperty("outEdges") Map<String, List<DependencyEdge>> outEdges) {
        this.nodes    = nodes    != null ? nodes    : new HashMap<>();
        this.outEdges = outEdges != null ? outEdges : new HashMap<>();
        this.jg = buildJGraph(this.nodes.keySet(), this.outEdges);
    }

    public RuleDependencyGraph() {
        this(new HashMap<>(), new HashMap<>());
    }

    private static DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> buildJGraph(
            Set<String> nodeKeys,
            Map<String, List<DependencyEdge>> outEdges) {

        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> g =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        for (String key : nodeKeys) g.addVertex(key);
        for (List<DependencyEdge> edges : outEdges.values()) {
            for (DependencyEdge e : edges) {
                // 保证两端顶点存在（容错）
                g.addVertex(e.getFromNodeKey());
                g.addVertex(e.getToNodeKey());
                DefaultWeightedEdge je = g.addEdge(e.getFromNodeKey(), e.getToNodeKey());
                if (je != null) g.setEdgeWeight(je, e.getFireCount());
            }
        }
        return g;
    }

    // ── 旧版兼容接口 ──────────────────────────────────────────────────────

    /** 返回所有节点的元数据映射。 */
    public Map<String, RuleNode> getNodes() { return nodes; }

    /** 返回所有出边映射（nodeKey → 出边列表）。 */
    public Map<String, List<DependencyEdge>> getOutEdges() { return outEdges; }

    /** 返回指定节点的出边列表；若不存在则返回空列表。 */
    public List<DependencyEdge> getOutEdges(String nodeKey) {
        return outEdges.getOrDefault(nodeKey, Collections.emptyList());
    }

    /** 根据 nodeKey 查找节点元数据。 */
    public RuleNode getNode(String nodeKey) { return nodes.get(nodeKey); }

    public int nodeCount() { return nodes.size(); }

    public int edgeCount() { return outEdges.values().stream().mapToInt(List::size).sum(); }

    // ── JGraphT 原生接口 ──────────────────────────────────────────────────

    /**
     * 返回底层 JGraphT 有向加权图，可直接使用任何 JGraphT 算法。
     * <ul>
     *   <li>顶点 = nodeKey（String）</li>
     *   <li>边权重 = fireCount</li>
     * </ul>
     */
    public DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> jgrapht() { return jg; }

    // ── 常用统计 / 遍历工具方法 ───────────────────────────────────────────

    /**
     * 返回入度为 0 的根节点列表。
     */
    public List<String> roots() {
        return jg.vertexSet().stream()
                .filter(v -> jg.inDegreeOf(v) == 0)
                .collect(Collectors.toList());
    }

    /**
     * 返回出度为 0 的叶子节点列表。
     */
    public List<String> leaves() {
        return jg.vertexSet().stream()
                .filter(v -> jg.outDegreeOf(v) == 0)
                .collect(Collectors.toList());
    }

    /**
     * BFS 层级映射：每个节点到其从最近根节点的最短跳数。
     * 使用 JGraphT {@link BreadthFirstIterator} 实现。
     */
    public Map<String, Integer> bfsDepth() {
        Map<String, Integer> depth = new HashMap<>();
        for (String root : roots()) {
            BreadthFirstIterator<String, DefaultWeightedEdge> it =
                    new BreadthFirstIterator<>(jg, root);
            while (it.hasNext()) {
                String v = it.next();
                int d = it.getDepth(v);
                // 取最小深度（多根情况）
                depth.merge(v, d, Math::min);
            }
        }
        // 孤立节点兜底
        for (String v : jg.vertexSet()) depth.putIfAbsent(v, 0);
        return depth;
    }

    /**
     * 生成树：对每个非根节点只保留权重最大的入边，形成一棵有根生成树。
     * 返回 child → parent 映射；根节点不出现在 key 中。
     */
    public Map<String, String> spanningTree() {
        Map<String, String> parent = new HashMap<>();
        for (String child : jg.vertexSet()) {
            DefaultWeightedEdge bestEdge = null;
            double bestWeight = -1;
            for (DefaultWeightedEdge e : jg.incomingEdgesOf(child)) {
                double w = jg.getEdgeWeight(e);
                if (w > bestWeight) { bestWeight = w; bestEdge = e; }
            }
            if (bestEdge != null) {
                parent.put(child, jg.getEdgeSource(bestEdge));
            }
        }
        return parent;
    }

    /**
     * 生成树深度：基于 {@link #spanningTree()} 计算每个节点在生成树中的深度。
     */
    public Map<String, Integer> spanningTreeDepth() {
        Map<String, String> parent = spanningTree();
        // 构建 children 关系
        Map<String, List<String>> children = new HashMap<>();
        for (Map.Entry<String, String> e : parent.entrySet()) {
            children.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        Map<String, Integer> depth = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        for (String root : roots()) { queue.add(root); depth.put(root, 0); }
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            int d = depth.get(cur);
            for (String child : children.getOrDefault(cur, List.of())) {
                if (!depth.containsKey(child)) {
                    depth.put(child, d + 1);
                    queue.add(child);
                }
            }
        }
        for (String v : jg.vertexSet()) depth.putIfAbsent(v, 0);
        return depth;
    }

    /**
     * 拓扑排序（Kahn BFS）。若图有环则返回局部结果。
     */
    public List<String> topologicalOrder() {
        List<String> result = new ArrayList<>();
        TopologicalOrderIterator<String, DefaultWeightedEdge> it =
                new TopologicalOrderIterator<>(jg);
        while (it.hasNext()) result.add(it.next());
        return result;
    }

    /**
     * 统计从 source 出发可达的所有节点（BFS）。
     */
    public Set<String> reachableFrom(String source) {
        if (!jg.containsVertex(source)) return Collections.emptySet();
        Set<String> visited = new LinkedHashSet<>();
        BreadthFirstIterator<String, DefaultWeightedEdge> it =
                new BreadthFirstIterator<>(jg, source);
        while (it.hasNext()) visited.add(it.next());
        return visited;
    }

    /**
     * 返回所有路径数（从所有根到所有叶），利用 JGraphT 做 DFS 计数。
     * 大图慎用（路径数可能指数级增长）。
     */
    public int countAllPaths() {
        int[] count = {0};
        for (String root : roots()) countPathsDFS(root, new HashSet<>(), count);
        return count[0];
    }

    private void countPathsDFS(String cur, Set<String> visited, int[] count) {
        if (jg.outDegreeOf(cur) == 0) { count[0]++; return; }
        visited.add(cur);
        for (DefaultWeightedEdge e : jg.outgoingEdgesOf(cur)) {
            String next = jg.getEdgeTarget(e);
            if (!visited.contains(next)) countPathsDFS(next, visited, count);
        }
        visited.remove(cur);
    }
}
