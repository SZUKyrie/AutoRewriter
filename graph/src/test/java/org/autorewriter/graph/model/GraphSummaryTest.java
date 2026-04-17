package org.autorewriter.graph.model;

import org.autorewriter.graph.builder.RuleGraphBuilder;
import org.autorewriter.rewriter.optimize.trace.OptimizationTrace;
import org.autorewriter.rewriter.optimize.trace.RuleApplicationStep;
import org.autorewriter.rewriter.rule.AutoRewriteRule;
import org.apache.calcite.rel.RelNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraphSummaryTest {

    private AutoRewriteRule mockRule(int ruleId) {
        AutoRewriteRule rule = mock(AutoRewriteRule.class);
        when(rule.getRuleId()).thenReturn(ruleId);
        RelNode src = mockNode(1000 + ruleId, "SrcTemplate");
        RelNode tgt = mockNode(2000 + ruleId, "TgtTemplate");
        when(rule.getSourceTemplate()).thenReturn(src);
        when(rule.getTargetTemplate()).thenReturn(tgt);
        return rule;
    }

    private RelNode mockNode(int id, String typeName) {
        RelNode n = mock(RelNode.class);
        when(n.getId()).thenReturn(id);
        when(n.getRelTypeName()).thenReturn(typeName);
        when(n.getInputs()).thenReturn(java.util.Collections.emptyList());
        return n;
    }

    @Test
    void testSummaryReport() {
        AutoRewriteRule rA = mockRule(0);
        AutoRewriteRule rB = mockRule(1);
        AutoRewriteRule rC = mockRule(2);

        RuleGraphBuilder builder = new RuleGraphBuilder();

        // trace1: A → B → C
        for (int i = 0; i < 3; i++) {
            OptimizationTrace t = new OptimizationTrace();
            t.addStep(new RuleApplicationStep(1, rA, mockNode(i,     "LogicalFilter"), mockNode(10 + i, "LogicalJoin")));
            t.addStep(new RuleApplicationStep(2, rB, mockNode(10 + i,"LogicalJoin"),   mockNode(20 + i, "LogicalProject")));
            t.addStep(new RuleApplicationStep(3, rC, mockNode(20 + i,"LogicalProject"),mockNode(30 + i, "LogicalFilter")));
            builder.record(t);
        }
        // trace2: A → C（直接跳过 B）
        for (int i = 0; i < 2; i++) {
            OptimizationTrace t = new OptimizationTrace();
            t.addStep(new RuleApplicationStep(1, rA, mockNode(100 + i, "LogicalFilter"), mockNode(110 + i, "LogicalProject")));
            t.addStep(new RuleApplicationStep(2, rC, mockNode(110 + i, "LogicalProject"),mockNode(120 + i, "LogicalFilter")));
            builder.record(t);
        }

        RuleDependencyGraph graph = builder.build();
        GraphSummary summary = new GraphSummary(graph);
        String report = summary.report(10);

        System.out.println(report);

        // 基本断言
        assertTrue(report.contains("RuleDependencyGraph Summary"));
        assertTrue(report.contains("Top 5 by Weighted Out-Degree"));
        assertTrue(report.contains("Top 5 by Weighted In-Degree"));
        assertTrue(report.contains("Top 5 by Observation Count"));

        // A 的出度应最高（连接 B 和 C）
        List<GraphSummary.NodeStats> stats = summary.compute();
        assertFalse(stats.isEmpty());
        // 第一名重要性最高
        assertTrue(stats.get(0).importance >= stats.get(stats.size() - 1).importance);
    }

    @Test
    void testEmptyGraph() {
        RuleDependencyGraph graph = new RuleDependencyGraph();
        String report = new GraphSummary(graph).report();
        assertTrue(report.contains("empty graph"));
    }
}
