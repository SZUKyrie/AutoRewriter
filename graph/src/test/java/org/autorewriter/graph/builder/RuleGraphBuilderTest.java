package org.autorewriter.graph.builder;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.rel.RelNode;
import org.autorewriter.graph.model.RuleDependencyGraph;
import org.autorewriter.graph.model.RuleNode;
import org.autorewriter.rewriter.optimize.trace.OptimizationTrace;
import org.autorewriter.rewriter.optimize.trace.RuleApplicationStep;
import org.autorewriter.rewriter.rule.AutoRewriteRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleGraphBuilderTest {

    private RuleGraphBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new RuleGraphBuilder();
    }

    /**
     * Two rules fire in same trace → directed edge A→B only (temporal order).
     */
    @Test
    void testSingleChainExtractsEdge() {
        AutoRewriteRule ruleA = mockAutoRewriteRule(0);
        AutoRewriteRule ruleB = mockAutoRewriteRule(1);

        RelNode matchedA = mockRelNode(1,  "LogicalFilter");
        RelNode afterA   = mockRelNode(10, "LogicalJoin");
        RelNode matchedB = mockRelNode(10, "LogicalJoin");
        RelNode afterB   = mockRelNode(20, "LogicalProject");

        RuleApplicationStep stepA = new RuleApplicationStep(1, ruleA, matchedA, afterA);
        RuleApplicationStep stepB = new RuleApplicationStep(2, ruleB, matchedB, afterB);

        OptimizationTrace trace = new OptimizationTrace();
        trace.addStep(stepA);
        trace.addStep(stepB);

        builder.record(trace);
        RuleDependencyGraph graph = builder.build();

        assertEquals(2, graph.nodeCount());
        // Directed: A→B only (A fires before B)
        assertEquals(1, graph.edgeCount());

        String nodeKeyA = RuleNode.keyOf(0, "LogicalFilter");
        String nodeKeyB = RuleNode.keyOf(1, "LogicalJoin");
        assertEquals(1, graph.getOutEdges(nodeKeyA).size());
        assertEquals(nodeKeyB, graph.getOutEdges(nodeKeyA).get(0).getToNodeKey());
        // No reverse edge
        assertEquals(0, graph.getOutEdges(nodeKeyB).size());
    }

    /**
     * Two traces with same A→B chain (different RelNode IDs) → edge fireCount=2
     */
    @Test
    void testAccumulatesAcrossMultipleTraces() {
        AutoRewriteRule ruleA = mockAutoRewriteRule(0);
        AutoRewriteRule ruleB = mockAutoRewriteRule(1);

        for (int i = 0; i < 2; i++) {
            RelNode matchedA = mockRelNode(i,          "LogicalFilter");
            RelNode afterA   = mockRelNode(10 + i * 100, "LogicalJoin");
            RelNode matchedB = mockRelNode(10 + i * 100, "LogicalJoin");
            RelNode afterB   = mockRelNode(99 + i,    "LogicalProject");

            OptimizationTrace trace = new OptimizationTrace();
            trace.addStep(new RuleApplicationStep(1, ruleA, matchedA, afterA));
            trace.addStep(new RuleApplicationStep(2, ruleB, matchedB, afterB));
            builder.record(trace);
        }

        RuleDependencyGraph graph = builder.build();
        String nodeKeyA = RuleNode.keyOf(0, "LogicalFilter");
        assertEquals(2, graph.getNode(nodeKeyA).getObservationCount());
        assertEquals(2, graph.getOutEdges(nodeKeyA).get(0).getFireCount());
        assertEquals(1.0, graph.getOutEdges(nodeKeyA).get(0).getProbability(2), 1e-9);
    }

    @Test
    void testNonAutoRewriteRuleStepsIgnored() {
        RelOptRule otherRule = mock(RelOptRule.class);
        OptimizationTrace trace = new OptimizationTrace();
        trace.addStep(new RuleApplicationStep(1, otherRule,
                mockRelNode(1, "A"), mockRelNode(2, "B")));

        builder.record(trace);
        RuleDependencyGraph graph = builder.build();

        assertEquals(0, graph.nodeCount());
        assertEquals(0, graph.edgeCount());
    }

    @Test
    void testEmptyTraceProducesEmptyGraph() {
        builder.record(new OptimizationTrace());
        RuleDependencyGraph graph = builder.build();
        assertEquals(0, graph.nodeCount());
        assertEquals(0, graph.edgeCount());
    }

    private AutoRewriteRule mockAutoRewriteRule(int ruleId) {
        AutoRewriteRule rule = mock(AutoRewriteRule.class);
        when(rule.getRuleId()).thenReturn(ruleId);
        RelNode srcNode = mockRelNode(1000 + ruleId, "SrcTemplate");
        RelNode tgtNode = mockRelNode(2000 + ruleId, "TgtTemplate");
        when(rule.getSourceTemplate()).thenReturn(srcNode);
        when(rule.getTargetTemplate()).thenReturn(tgtNode);
        return rule;
    }

    private RelNode mockRelNode(int id, String typeName) {
        RelNode node = mock(RelNode.class);
        when(node.getId()).thenReturn(id);
        when(node.getRelTypeName()).thenReturn(typeName);
        when(node.getInputs()).thenReturn(java.util.Collections.emptyList());
        return node;
    }
}
