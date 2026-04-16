package org.autorewriter.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory rule dependency graph — fully backed by JGraphT.
 *
 * <p>顶点 = nodeKey（{@code "ruleId:matchedNodeSignature"}），
 * JGraphT 边权重 = fireCount。
 *
 * <p>节点元数据（observationCount 等）存在 {@link #nodeMetadata} 中，
 * 可通过 {@link #getNode(String)} 查询。
 */
public class RuleDependencyGraph {

    // ── 核心数据 ──────────────────────────────────────────────────────────

    /** JGraphT 有向加权图，顶点 = nodeKey，边权重 = fireCount */
    @JsonIgnore
    private final DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> jg;

    /** nodeKey → RuleNode 元数据 */
    private final Map<String, RuleNode> nodeMetadata;

    /** nodeKey → 出边列表（供 JSON 序列化 / 旧兼容层） */
    private final Map<String, List<DependencyEdge>> edgeMap;

    // ── 构造 ──────────────────────────────────────────────────────────────

    @JsonCreator
    public RuleDependencyGraph(
            @JsonProperty("nodes")    Map<String, RuleNode>            nodes,
            @JsonProperty("outEdges") Map<String, List<DependencyEdge>> outEdges) {
        this.nodeMetadata = nodes    != null ? new LinkedHashMap<>(nodes)    : new LinkedHashMap<>();
        this.edgeMap      = outEdges != null ? new LinkedHashMap<>(outEdges) : new LinkedHashMap<>();
        this.jg = buildJGraph(this.nodeMetadata.keySet(), this.edgeMap);
    }

    public RuleDependencyGraph() {
        this(new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    private static DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> buildJGraph(
            Set<String> nodeKeys,
            Map<String, List<DependencyEdge>> outEdges) {

        DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> g =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        for (String key : nodeKeys) g.addVertex(key);
        for (List<DependencyEdge> edges : outEdges.values()) {
            for (DependencyEdge e : edges) {
                g.addVertex(e.getFromNodeKey());
                g.addVertex(e.getToNodeKey());
                DefaultWeightedEdge je = g.addEdge(e.getFromNodeKey(), e.getToNodeKey());
                if (je != null) g.setEdgeWeight(je, e.getFireCount());
            }
        }
        return g;
    }

    // ── JSON 序列化用（供 GraphSerializer 读取） ──────────────────────────

    @JsonProperty("nodes")
    public Map<String, RuleNode> getNodes() { return Collections.unmodifiableMap(nodeMetadata); }

    @JsonProperty("outEdges")
    public Map<String, List<DependencyEdge>> getOutEdges() { return Collections.unmodifiableMap(edgeMap); }

    // ── JGraphT 原生接口 ──────────────────────────────────────────────────

    /**
     * 返回底层 JGraphT 有向加权图。
     * 顶点 = nodeKey，边权重 = fireCount。
     */
    @JsonIgnore
    public DefaultDirectedWeightedGraph<String, DefaultWeightedEdge> jgrapht() { return jg; }

    // ── 常用查询 ──────────────────────────────────────────────────────────

    /** 节点数。 */
    public int nodeCount() { return jg.vertexSet().size(); }

    /** 边数。 */
    public int edgeCount() { return jg.edgeSet().size(); }

    /** 根据 nodeKey 查询节点元数据。 */
    public RuleNode getNode(String nodeKey) { return nodeMetadata.get(nodeKey); }

    /** 返回指定节点的出边列表；不存在则返回空列表。 */
    public List<DependencyEdge> getOutEdgesOf(String nodeKey) {
        return edgeMap.getOrDefault(nodeKey, Collections.emptyList());
    }

    /** 入度为 0 的根节点列表。 */
    public List<String> roots() {
        return jg.vertexSet().stream()
                .filter(v -> jg.inDegreeOf(v) == 0)
                .collect(Collectors.toList());
    }

    /** 出度为 0 的叶节点列表。 */
    public List<String> leaves() {
        return jg.vertexSet().stream()
                .filter(v -> jg.outDegreeOf(v) == 0)
                .collect(Collectors.toList());
    }

    /**
     * BFS 最短路径层级（从最近根节点到各顶点的跳数）。
     */
    public Map<String, Integer> bfsDepth() {
        Map<String, Integer> depth = new HashMap<>();
        for (String root : roots()) {
            BreadthFirstIterator<String, DefaultWeightedEdge> it =
                    new BreadthFirstIterator<>(jg, root);
            while (it.hasNext()) {
                String v = it.next();
                int d = it.getDepth(v);
                depth.merge(v, d, Math::min);
            }
        }
        for (String v : jg.vertexSet()) depth.putIfAbsent(v, 0);
        return depth;
    }

    /**
     * 生成树：对每个非根节点只保留权重最大的入边（child → parent 映射）。
     */
    public Map<String, String> spanningTree() {
        Map<String, String> parent = new HashMap<>();
        for (String child : jg.vertexSet()) {
            DefaultWeightedEdge best = null;
            double bestW = -1;
            for (DefaultWeightedEdge e : jg.incomingEdgesOf(child)) {
                double w = jg.getEdgeWeight(e);
                if (w > bestW) { bestW = w; best = e; }
            }
            if (best != null) parent.put(child, jg.getEdgeSource(best));
        }
        return parent;
    }

    /**
     * 生成树深度（基于 {@link #spanningTree()}）。
     */
    public Map<String, Integer> spanningTreeDepth() {
        Map<String, String> parent = spanningTree();
        Map<String, List<String>> children = new HashMap<>();
        for (Map.Entry<String, String> e : parent.entrySet())
            children.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());

        Map<String, Integer> depth = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        for (String root : roots()) { queue.add(root); depth.put(root, 0); }
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            int d = depth.get(cur);
            for (String child : children.getOrDefault(cur, Collections.emptyList())) {
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
     * 拓扑排序（使用 JGraphT {@link TopologicalOrderIterator}）。
     */
    public List<String> topologicalOrder() {
        List<String> result = new ArrayList<>();
        new TopologicalOrderIterator<>(jg).forEachRemaining(result::add);
        return result;
    }

    /**
     * 从指定顶点出发的 BFS 可达集合。
     */
    public Set<String> reachableFrom(String source) {
        if (!jg.containsVertex(source)) return Collections.emptySet();
        Set<String> visited = new LinkedHashSet<>();
        new BreadthFirstIterator<>(jg, source).forEachRemaining(visited::add);
        return visited;
    }

    /**
     * 判断给定节点的出边中，fireCount 最大的那条所指向的邻居。
     * 用于 GraphModule.rankRules 等推荐场景。
     */
    public Optional<String> bestSuccessor(String nodeKey) {
        return jg.outgoingEdgesOf(nodeKey).stream()
                .max(Comparator.comparingDouble(jg::getEdgeWeight))
                .map(jg::getEdgeTarget);
    }

    /**
     * 返回出边按 fireCount 降序排列的邻居列表。
     */
    public List<String> rankedSuccessors(String nodeKey) {
        if (!jg.containsVertex(nodeKey)) return Collections.emptyList();
        return jg.outgoingEdgesOf(nodeKey).stream()
                .sorted(Comparator.comparingDouble(jg::getEdgeWeight).reversed())
                .map(jg::getEdgeTarget)
                .collect(Collectors.toList());
    }
}
