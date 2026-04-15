package org.autorewriter.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A node in the rule dependency graph.
 *
 * <p>A node represents one specific firing of an AutoRewriteRule at a particular
 * query sub-plan position. The same rule fired at different positions in the plan
 * tree produces distinct nodes, distinguished by {@code matchedNodeSignature}.
 *
 * <p>Node key: {@code ruleId + ":" + matchedNodeSignature}
 */
@Getter
public class RuleNode {

    /** Unique string key: "ruleId:matchedNodeSignature" */
    private final String nodeKey;

    private final int    ruleId;
    private final String sourceTemplateSignature;
    private final String targetTemplateSignature;

    /** Structural signature of the matched query sub-plan node (distinguishes firing positions). */
    private final String matchedNodeSignature;

    private       int    observationCount;

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
    }

    public void incrementObservation() {
        this.observationCount++;
    }

    /** Build the node key from ruleId and matchedNodeSignature. */
    public static String keyOf(int ruleId, String matchedNodeSignature) {
        return ruleId + ":" + matchedNodeSignature;
    }
}
