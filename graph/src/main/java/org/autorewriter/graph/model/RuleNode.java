package org.autorewriter.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A node in the rule dependency graph, representing a single AutoRewriteRule.
 */
@Getter
public class RuleNode {

    private final int    ruleId;
    private final String sourceTemplateSignature;
    private final String targetTemplateSignature;
    private       int    observationCount;

    @JsonCreator
    public RuleNode(
            @JsonProperty("ruleId")                  int    ruleId,
            @JsonProperty("sourceTemplateSignature") String sourceTemplateSignature,
            @JsonProperty("targetTemplateSignature") String targetTemplateSignature,
            @JsonProperty("observationCount")        int    observationCount) {
        this.ruleId                  = ruleId;
        this.sourceTemplateSignature = sourceTemplateSignature;
        this.targetTemplateSignature = targetTemplateSignature;
        this.observationCount        = observationCount;
    }

    /** Increment observation count by 1. */
    public void incrementObservation() {
        this.observationCount++;
    }
}
