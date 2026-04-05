package org.autorewriter.rewriter.analyze;

import org.autorewriter.rewriter.optimize.costBaseOpt.DistinctAggregateStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link RuleAnalysisContext}, focusing on {@link RuleAnalysisContext#isNoOp()}
 * behavior with constraint-based equivalence detection.
 */
class RuleAnalysisContextTest {

    @Test
    @DisplayName("isNoOp: stripped DISTINCT rule with constraint equivalences should be detected as no-op")
    void testIsNoOpWithStrippedDistinctAndConstraintEquivalences() {
        // Proj*<a0 s0>(Input<t0>) | Proj<a1 s1>(Input<t1>) | AttrsSub(a0,t0);Unique(t0,a0);TableEq(t1,t0);AttrsEq(a1,a0);SchemaEq(s1,s0)
        //
        // After stripping DISTINCT aggregate from source:
        //   Source: LogicalProject<a0>(LogicalTableScan<t0>)
        //   Target: LogicalProject<a1>(LogicalTableScan<t1>)
        //
        // Constraints declare: t1≡t0, a1≡a0, s1≡s0
        // So this is an identity rewrite — isNoOp() should return true.
        String ruleStr = "Proj*<a0 s0>(Input<t0>)|Proj<a1 s1>(Input<t1>)"
                + "|AttrsSub(a0,t0);Unique(t0,a0);TableEq(t1,t0);AttrsEq(a1,a0);SchemaEq(s1,s0)";
        RuleAnalysisContext ctx = RuleAnalyzer.analyze(ruleStr);

        // Before stripping: source has Aggregate, target has Project — not a no-op
        assertFalse(ctx.isNoOp(),
                "Original rule with Aggregate source vs Project target should not be a no-op");

        // Strip DISTINCT aggregate from source
        RuleAnalysisContext stripped = DistinctAggregateStripper.stripBoth(ctx);

        // After stripping: both are Project(TableScan), and constraints make them equivalent
        assertTrue(stripped.isNoOp(),
                "Stripped rule with TableEq(t1,t0), AttrsEq(a1,a0), SchemaEq(s1,s0) "
                        + "should be detected as no-op");
    }

    @Test
    @DisplayName("isNoOp: rule with identical placeholders (no constraints needed) should be detected as no-op")
    void testIsNoOpWithIdenticalPlaceholders() {
        // Source and target use the same placeholder names → trivially a no-op
        String ruleStr = "Proj<a0 s0>(Input<t0>)|Proj<a0 s0>(Input<t0>)|";
        RuleAnalysisContext ctx = RuleAnalyzer.analyze(ruleStr);

        assertTrue(ctx.isNoOp(),
                "Rule with identical source and target placeholders should be a no-op");
    }

    @Test
    @DisplayName("isNoOp: rule with different placeholders but NO equality constraints should NOT be a no-op")
    void testIsNoOpWithDifferentPlaceholdersNoConstraints() {
        // Source uses a0/t0, target uses a1/t1, but no equality constraints link them
        String ruleStr = "Proj<a0 s0>(Input<t0>)|Proj<a1 s1>(Input<t1>)|";
        RuleAnalysisContext ctx = RuleAnalyzer.analyze(ruleStr);

        assertFalse(ctx.isNoOp(),
                "Rule with different placeholders and no equality constraints should not be a no-op");
    }

    @Test
    @DisplayName("isNoOp: rule with structural difference should NOT be a no-op even with constraints")
    void testIsNoOpWithStructuralDifference() {
        // Source: Filter(Project(Input))  vs  Target: Project(Input)
        // Even with equality constraints, structurally different trees are not no-ops
        String ruleStr = "Filter<p0>(Proj<a0 s0>(Input<t0>))|Proj<a1 s1>(Input<t1>)"
                + "|TableEq(t1,t0);AttrsEq(a1,a0);SchemaEq(s1,s0)";
        RuleAnalysisContext ctx = RuleAnalyzer.analyze(ruleStr);

        assertFalse(ctx.isNoOp(),
                "Rule with different tree structure should not be a no-op");
    }

    @Test
    @DisplayName("isNoOp: rule with partial equality constraints should NOT be a no-op")
    void testIsNoOpWithPartialConstraints() {
        // Only TableEq is present but not AttrsEq — so a1 ≠ a0, not a full identity
        String ruleStr = "Proj<a0 s0>(Input<t0>)|Proj<a1 s1>(Input<t1>)"
                + "|TableEq(t1,t0)";
        RuleAnalysisContext ctx = RuleAnalyzer.analyze(ruleStr);

        assertFalse(ctx.isNoOp(),
                "Rule with only partial equality constraints (TableEq but not AttrsEq) "
                        + "should not be a no-op");
    }

    @Test
    @DisplayName("isNoOp: non-trivial rewrite (DISTINCT removal with Unique) is NOT a no-op before stripping")
    void testIsNoOpDistinctRemovalBeforeStripping() {
        // This is the actual DISTINCT removal rule — source has Aggregate, target doesn't
        String ruleStr = "Proj*<a0 s0>(Input<t0>)|Proj<a1 s1>(Input<t1>)"
                + "|AttrsSub(a0,t0);Unique(t0,a0);TableEq(t1,t0);AttrsEq(a1,a0);SchemaEq(s1,s0)";
        RuleAnalysisContext ctx = RuleAnalyzer.analyze(ruleStr);

        // Source has LogicalAggregate + LogicalProject, target has only LogicalProject
        assertFalse(ctx.isNoOp(),
                "DISTINCT removal rule should not be a no-op (structurally different)");
    }

    @Test
    @DisplayName("isNoOp: different tree structures with some constraints should NOT be a no-op")
    void testIsNoOpDifferentStructuresNotNoOp() {
        // A rule where source and target have the same placeholders but
        // the DISTINCT removal makes them structurally different even
        // with all equality constraints — only stripping makes them equal
        String ruleStr = "Proj*<a0 s0>(Input<t0>)|Proj<a1 s1>(Input<t1>)"
                + "|AttrsSub(a0,t0);Unique(t0,a0);TableEq(t1,t0);AttrsEq(a1,a0);SchemaEq(s1,s0)";
        RuleAnalysisContext ctx = RuleAnalyzer.analyze(ruleStr);

        // The unstripped rule: Aggregate(Project(TableScan)) vs Project(TableScan)
        // These are structurally different — NOT a no-op
        assertFalse(ctx.isNoOp(),
                "Rule with structurally different source (Aggregate) and target (Project) "
                        + "should not be a no-op even with equality constraints");
    }
}
