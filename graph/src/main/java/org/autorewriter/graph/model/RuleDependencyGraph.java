package org.autorewriter.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.*;

/**
 * In-memory rule dependency graph.
 * Nodes are AutoRewriteRules; edges represent observed A→B firing sequences.
 */
@Getter
public class RuleDependencyGraph {

    /** ruleId → RuleNode */
    private final Map<Integer, RuleNode> nodes;

    /** ruleId → outgoing edges */
    private final Map<Integer, List<DependencyEdge>> outEdges;

    @JsonCreator
    public RuleDependencyGraph(
            @JsonProperty("nodes")    Map<Integer, RuleNode>            nodes,
            @JsonProperty("outEdges") Map<Integer, List<DependencyEdge>> outEdges) {
        this.nodes    = nodes    != null ? nodes    : new HashMap<>();
        this.outEdges = outEdges != null ? outEdges : new HashMap<>();
    }

    public RuleDependencyGraph() {
        this(new HashMap<>(), new HashMap<>());
    }

    /** Returns outgoing edges for ruleId, or empty list if none. */
    public List<DependencyEdge> getOutEdges(int ruleId) {
        return outEdges.getOrDefault(ruleId, Collections.emptyList());
    }

    /** Returns the node for ruleId, or null if not present. */
    public RuleNode getNode(int ruleId) {
        return nodes.get(ruleId);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return outEdges.values().stream().mapToInt(List::size).sum();
    }
}
