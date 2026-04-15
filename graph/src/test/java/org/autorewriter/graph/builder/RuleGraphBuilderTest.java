package org.autorewriter.graph.builder;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.rel.RelNode;
import org.autorewriter.graph.model.RuleDependencyGraph;
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

    @Test
    void testSingleChainExtractsEdge() {
        AutoRewriteRule ruleA = mockAutoRewriteRule(0);
        AutoRewriteRule ruleB = mockAutoRewriteRule(1);

        RelNode afterA  = mockRelNode(10);
        RelNode beforeB = mockRelNode(10); // same id as afterA -> chain

        RuleApplicationStep stepA = new RuleApplicationStep(1, ruleA, mockRelNode(1), afterA);
        RuleApplicationStep stepB = new RuleApplicationStep(2, ruleB, beforeB, mockRelNode(20));

        OptimizationTrace trace = new OptimizationTrace();
        trace.addStep(stepA);
        trace.addStep(stepB);

        builder.record(trace);
        RuleDependencyGraph graph = builder.build();

        assertEquals(2, graph.nodeCount());
        assertEquals(1, graph.edgeCount());
        assertEquals(1, graph.getOutEdges(0).size());
        assertEquals(1, graph.getOutEdges(0).get(0).getToRuleId());
        assertEquals(1, graph.getOutEdges(0).get(0).getFireCount());
    }

    @Test
    void testAccumulatesAcrossMultipleTraces() {
        AutoRewriteRule ruleA = mockAutoRewriteRule(0);
        AutoRewriteRule ruleB = mockAutoRewriteRule(1);

        for (int i = 0; i < 2; i++) {
            RelNode afterA  = mockRelNode(10 + i * 100);
            RelNode beforeB = mockRelNode(10 + i * 100);
            OptimizationTrace trace = new OptimizationTrace();
            trace.addStep(new RuleApplicationStep(1, ruleA, mockRelNode(i), afterA));
            trace.addStep(new RuleApplicationStep(2, ruleB, beforeB, mockRelNode(99 + i)));
            builder.record(trace);
        }

        RuleDependencyGraph graph = builder.build();
        assertEquals(2, graph.getNode(0).getObservationCount());
        assertEquals(2, graph.getOutEdges(0).get(0).getFireCount());
        assertEquals(1.0, graph.getOutEdges(0).get(0).getProbability(2), 1e-9);
    }

    @Test
    void testNonAutoRewriteRuleStepsIgnored() {
        RelOptRule otherRule = mock(RelOptRule.class);
        OptimizationTrace trace = new OptimizationTrace();
        trace.addStep(new RuleApplicationStep(1, otherRule, mockRelNode(1), mockRelNode(2)));

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
        RelNode sourceNode = mockRelNode(1000 + ruleId);
        RelNode targetNode = mockRelNode(2000 + ruleId);
        when(rule.getSourceTemplate()).thenReturn(sourceNode);
        when(rule.getTargetTemplate()).thenReturn(targetNode);
        return rule;
    }

    private RelNode mockRelNode(int id) {
        RelNode node = mock(RelNode.class);
        when(node.getId()).thenReturn(id);
        when(node.getRelTypeName()).thenReturn("MockNode");
        when(node.getInputs()).thenReturn(java.util.Collections.emptyList());
        return node;
    }
}
