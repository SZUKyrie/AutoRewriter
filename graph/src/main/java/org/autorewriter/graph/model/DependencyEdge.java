package org.autorewriter.graph.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A directed edge in the rule dependency graph.
 * fromRuleId → toRuleId means: after fromRule fired, toRule was subsequently observed to fire.
 */
@Getter
public class DependencyEdge {

    private final int    fromRuleId;
    private final int    toRuleId;
    private       int    fireCount;
    private       double totalBenefit;

    @JsonCreator
    public DependencyEdge(
            @JsonProperty("fromRuleId")   int    fromRuleId,
            @JsonProperty("toRuleId")     int    toRuleId,
            @JsonProperty("fireCount")    int    fireCount,
            @JsonProperty("totalBenefit") double totalBenefit) {
        this.fromRuleId   = fromRuleId;
        this.toRuleId     = toRuleId;
        this.fireCount    = fireCount;
        this.totalBenefit = totalBenefit;
    }

    /** Record one more observation of this A→B transition. */
    public void recordFire(double benefit) {
        this.fireCount++;
        this.totalBenefit += benefit;
    }

    /** P(toRule fires | fromRule fired) */
    @JsonProperty("probability")
    public double getProbability(int fromObservationCount) {
        if (fromObservationCount == 0) return 0.0;
        return (double) fireCount / fromObservationCount;
    }

    /** E[benefit(toRule) | fromRule fired] */
    @JsonProperty("avgBenefit")
    public double getAvgBenefit() {
        if (fireCount == 0) return 0.0;
        return totalBenefit / fireCount;
    }
}
