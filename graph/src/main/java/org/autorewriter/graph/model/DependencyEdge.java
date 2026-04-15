package org.autorewriter.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A directed edge in the rule dependency graph.
 *
 * <p>{@code fromNodeKey} → {@code toNodeKey} means: the firing represented by
 * {@code fromNodeKey} produced a RelNode that was subsequently matched and
 * consumed by the firing represented by {@code toNodeKey}.
 *
 * <p>Node keys have the form {@code "ruleId:matchedNodeSignature"}.
 */
@Getter
public class DependencyEdge {

    private final String fromNodeKey;
    private final String toNodeKey;
    private       int    fireCount;
    private       double totalBenefit;

    @JsonCreator
    public DependencyEdge(
            @JsonProperty("fromNodeKey")  String fromNodeKey,
            @JsonProperty("toNodeKey")    String toNodeKey,
            @JsonProperty("fireCount")    int    fireCount,
            @JsonProperty("totalBenefit") double totalBenefit) {
        this.fromNodeKey  = fromNodeKey;
        this.toNodeKey    = toNodeKey;
        this.fireCount    = fireCount;
        this.totalBenefit = totalBenefit;
    }

    public void recordFire(double benefit) {
        this.fireCount++;
        this.totalBenefit += benefit;
    }

    /** P(toNode fires | fromNode fired) */
    @JsonProperty("probability")
    public double getProbability(int fromObservationCount) {
        if (fromObservationCount == 0) return 0.0;
        return (double) fireCount / fromObservationCount;
    }

    /** E[benefit(toNode) | fromNode fired] */
    @JsonProperty("avgBenefit")
    public double getAvgBenefit() {
        if (fireCount == 0) return 0.0;
        return totalBenefit / fireCount;
    }
}
