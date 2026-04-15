package org.autorewriter.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.*;

/**
 * In-memory rule dependency graph.
 *
 * <p>Nodes are keyed by {@code "ruleId:matchedNodeSignature"}, so the same rule
 * fired at different query sub-plan positions appears as distinct nodes.
 * Edges represent observed A→B firing sequences connected by RelNode ID linkage
 * (A.producedRelNode.getId() == B.matchedRelNode.getId()).
 */
@Getter
public class RuleDependencyGraph {

    /** nodeKey → RuleNode */
    private final Map<String, RuleNode> nodes;

    /** nodeKey → outgoing edges */
    private final Map<String, List<DependencyEdge>> outEdges;

    @JsonCreator
    public RuleDependencyGraph(
            @JsonProperty("nodes")    Map<String, RuleNode>            nodes,
            @JsonProperty("outEdges") Map<String, List<DependencyEdge>> outEdges) {
        this.nodes    = nodes    != null ? nodes    : new HashMap<>();
        this.outEdges = outEdges != null ? outEdges : new HashMap<>();
    }

    public RuleDependencyGraph() {
        this(new HashMap<>(), new HashMap<>());
    }

    public List<DependencyEdge> getOutEdges(String nodeKey) {
        return outEdges.getOrDefault(nodeKey, Collections.emptyList());
    }

    public RuleNode getNode(String nodeKey) {
        return nodes.get(nodeKey);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return outEdges.values().stream().mapToInt(List::size).sum();
    }
}
