package org.autorewriter.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.jgrapht.graph.DefaultEdge;

/**
 * A node in the rule dependency graph.
 *
 * <p>Node key: {@code ruleId + ":" + matchedNodeSignature}
 */
@Getter
public class RuleNode extends DefaultEdge {

    private final String nodeKey;
    private final int    ruleId;
    private final String sourceTemplateSignature;
    private final String targetTemplateSignature;
    private final String matchedNodeSignature;

    /**
     * Minimum trace position (0-indexed) at which this node was first observed.
     * Used for rank=same tree layout. -1 = unknown.
     */
    private int rank;

    private int observationCount;

    @JsonCreator
    public RuleNode(
            @JsonProperty("nodeKey")                 String nodeKey,
            @JsonProperty("ruleId")                  int    ruleId,
            @JsonProperty("sourceTemplateSignature") String sourceTemplateSignature,
            @JsonProperty("targetTemplateSignature") String targetTemplateSignature,
            @JsonProperty("matchedNodeSignature")    String matchedNodeSignature,
            @JsonProperty("observationCount")        int    observationCount) {
        this.nodeKey                 = nodeKey;
        this.ruleId                  = ruleId;
        this.sourceTemplateSignature = sourceTemplateSignature;
        this.targetTemplateSignature = targetTemplateSignature;
        this.matchedNodeSignature    = matchedNodeSignature;
        this.observationCount        = observationCount;
        this.rank                    = -1;
    }

    public void incrementObservation() { this.observationCount++; }

    /** Update rank to the minimum observed position. */
    public void updateRank(int position) {
        if (this.rank < 0 || position < this.rank) {
            this.rank = position;
        }
    }

    public void setRank(int rank) { this.rank = rank; }

    public static String keyOf(int ruleId, String matchedNodeSignature) {
        return ruleId + ":" + matchedNodeSignature;
    }
}
