package org.autorewriter.graph.operator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 规则算子图（Rule Operator Graph）。
 *
 * <p>两类节点/边：
 * <ul>
 *   <li><b>节点</b>：规则模版中出现的 Calcite {@code RelNode}，按 explain 去重。</li>
 *   <li><b>结构边</b>：算子树父子关系（{@link OperatorEdge.EdgeType#STRUCTURAL}）。</li>
 *   <li><b>转换边</b>：源模版根 → 目标模版根（{@link OperatorEdge.EdgeType#TRANSFORM}），
 *       边上携带规则 ID 与约束描述。</li>
 * </ul>
 *
 * <p>底层由 JGraphT {@link DefaultDirectedGraph} 支撑，可直接使用 JGraphT 的
 * 任意算法（最短路径、连通分量、圈检测等）。
 */
@Getter
public class RuleOperatorGraph {

    /** nodeId → OperatorNode */
    private final Map<String, OperatorNode> nodes;

    /** 所有边（结构边 + 转换边） */
    private final List<OperatorEdge> edges;

    /** JGraphT 底层图，顶点 = nodeId，边 = OperatorEdge */
    private final DefaultDirectedGraph<String, OperatorEdge> jgrapht;

    @JsonCreator
    public RuleOperatorGraph(
            @JsonProperty("nodes") Map<String, OperatorNode> nodes,
            @JsonProperty("edges") List<OperatorEdge>        edges) {
        this.nodes = nodes != null ? nodes : new LinkedHashMap<>();
        this.edges = edges != null ? edges : new ArrayList<>();
        this.jgrapht = buildJGraph(this.nodes.keySet(), this.edges);
    }

    public RuleOperatorGraph() {
        this(new LinkedHashMap<>(), new ArrayList<>());
    }

    // ── 便捷查询 ────────────────────────────────────────────────────────────

    public OperatorNode getNode(String nodeId)       { return nodes.get(nodeId); }
    public int          nodeCount()                  { return nodes.size(); }
    public long         edgeCount()                  { return edges.size(); }

    /** 所有结构边。 */
    public List<OperatorEdge> structuralEdges() {
        return edges.stream()
                .filter(e -> e.getType() == OperatorEdge.EdgeType.STRUCTURAL)
                .collect(Collectors.toList());
    }

    /** 所有转换边。 */
    public List<OperatorEdge> transformEdges() {
        return edges.stream()
                .filter(e -> e.getType() == OperatorEdge.EdgeType.TRANSFORM)
                .collect(Collectors.toList());
    }

    /** 从指定节点出发的所有出边。 */
    public List<OperatorEdge> outEdges(String nodeId) {
        return edges.stream()
                .filter(e -> e.getFromNodeId().equals(nodeId))
                .collect(Collectors.toList());
    }

    /** 以指定节点为终点的所有入边。 */
    public List<OperatorEdge> inEdges(String nodeId) {
        return edges.stream()
                .filter(e -> e.getToNodeId().equals(nodeId))
                .collect(Collectors.toList());
    }

    /** 根节点（入度 = 0 的节点）。 */
    public List<String> roots() {
        Set<String> targets = edges.stream()
                .map(OperatorEdge::getToNodeId)
                .collect(Collectors.toSet());
        return nodes.keySet().stream()
                .filter(id -> !targets.contains(id))
                .collect(Collectors.toList());
    }

    // ── JGraphT 构建 ─────────────────────────────────────────────────────────

    private static DefaultDirectedGraph<String, OperatorEdge> buildJGraph(
            Set<String> nodeIds, List<OperatorEdge> edgeList) {
        DefaultDirectedGraph<String, OperatorEdge> g =
                new DefaultDirectedGraph<>(OperatorEdge.class);
        for (String id : nodeIds) g.addVertex(id);
        for (OperatorEdge e : edgeList) {
            g.addVertex(e.getFromNodeId());
            g.addVertex(e.getToNodeId());
            // JGraphT 不允许重复边，用 try-catch 忽略
            try { g.addEdge(e.getFromNodeId(), e.getToNodeId(), e); }
            catch (IllegalArgumentException ignored) { /* 已存在，跳过 */ }
        }
        return g;
    }
}
