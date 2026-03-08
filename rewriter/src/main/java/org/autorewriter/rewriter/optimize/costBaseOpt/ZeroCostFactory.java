package org.autorewriter.rewriter.optimize.costBaseOpt;

import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptCostFactory;
import org.apache.calcite.plan.RelOptUtil;

/**
 * A cost factory that returns zero for all computed costs, forcing VolcanoPlanner
 * to treat all valid plans as equally cheap and explore all alternatives.
 *
 * <p>Infinite costs are preserved so the planner can still distinguish valid
 * plans (cost=0) from invalid/incomplete plans (cost=infinite).
 */
public class ZeroCostFactory implements RelOptCostFactory {

    public static final ZeroCostFactory INSTANCE = new ZeroCostFactory();

    @Override
    public RelOptCost makeCost(double dRows, double dCpu, double dIo) {
        return new ZeroCost(false);
    }

    @Override
    public RelOptCost makeHugeCost() {
        return new ZeroCost(true);
    }

    @Override
    public RelOptCost makeInfiniteCost() {
        return new ZeroCost(true);
    }

    @Override
    public RelOptCost makeTinyCost() {
        return new ZeroCost(false);
    }

    @Override
    public RelOptCost makeZeroCost() {
        return new ZeroCost(false);
    }

    /**
     * A RelOptCost that is always zero (or infinite).
     * Zero costs are equal to each other; infinite costs are greater than zero costs.
     */
    private static class ZeroCost implements RelOptCost {
        private final boolean infinite;

        ZeroCost(boolean infinite) {
            this.infinite = infinite;
        }

        @Override public double getRows() { return infinite ? Double.MAX_VALUE : 0; }
        @Override public double getCpu()  { return infinite ? Double.MAX_VALUE : 0; }
        @Override public double getIo()   { return infinite ? Double.MAX_VALUE : 0; }
        @Override public boolean isInfinite() { return infinite; }

        @Override
        public boolean isLe(RelOptCost other) {
            if (infinite) return other.isInfinite();
            return true; // 0 <= anything
        }

        @Override
        public boolean isLt(RelOptCost other) {
            if (infinite) return false;
            return other.isInfinite();
        }

        @Override
        public boolean equals(RelOptCost other) {
            if (other instanceof ZeroCost) {
                return this.infinite == ((ZeroCost) other).infinite;
            }
            return !infinite && !other.isInfinite()
                    && other.getRows() == 0 && other.getCpu() == 0 && other.getIo() == 0;
        }

        @Override
        public boolean isEqWithEpsilon(RelOptCost other) {
            return equals(other);
        }

        @Override
        public RelOptCost minus(RelOptCost other) {
            return this;
        }

        @Override
        public RelOptCost multiplyBy(double factor) {
            return this;
        }

        @Override
        public double divideBy(RelOptCost other) {
            return 1.0;
        }

        @Override
        public RelOptCost plus(RelOptCost other) {
            if (infinite || other.isInfinite()) return new ZeroCost(true);
            return new ZeroCost(false);
        }

        @Override
        public String toString() {
            return infinite ? "{inf}" : "{0}";
        }

        @Override
        public int hashCode() {
            return infinite ? 1 : 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ZeroCost) return this.infinite == ((ZeroCost) obj).infinite;
            return false;
        }
    }
}
