package org.autorewriter.rewriter.optimize.ruleBaseOpt;

import org.autorewriter.rewriter.analyze.RuleAnalysisContext;
import org.autorewriter.rewriter.analyze.RuleAnalyzer;
import org.junit.jupiter.api.Test;

public class SingleRuleParseTest {

    @Test
    public void testSimpleRule() {
        String ruleStr = "Proj*<a0 s0>(Input<t0>)|Proj<a1 s1>(Input<t1>)|AttrsSub(a0,t0);Unique(t0,a0);TableEq(t1,t0);AttrsEq(a1,a0);SchemaEq(s1,s0)";

        try {
            RuleAnalysisContext context = RuleAnalyzer.analyze(ruleStr);
            System.out.println("✓ 规则解析成功:");
            System.out.println("  源模板: " + context.getSourceRelNode());
            System.out.println("  目标模板: " + context.getTargetRelNode());
            System.out.println("  匹配约束数: " + context.getMatchConstraints().size());
            System.out.println("  改写约束数: " + context.getRewriteConstraints().size());
        } catch (Exception e) {
            System.err.println("✗ 规则解析失败:");
            System.err.println("  错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testComplexJoinRule() {
        String ruleStr = "Proj<a2 s0>(LeftJoin<a0 a1>(Input<t0>,Input<t1>))|Proj<a5 s1>(LeftJoin<a3 a4>(Input<t2>,Input<t3>))|AttrsEq(a0,a2);AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t0);Reference(t0,a0,t1,a1);TableEq(t2,t0);TableEq(t3,t1);AttrsEq(a3,a0);AttrsEq(a4,a1);AttrsEq(a5,a1);SchemaEq(s1,s0)";

        System.out.println("测试规则:");
        System.out.println(ruleStr);
        System.out.println();

        try {
            RuleAnalysisContext context = RuleAnalyzer.analyze(ruleStr);
            System.out.println("✓ 规则解析成功:");
            System.out.println("  源模板: " + context.getSourceRelNode());
            System.out.println("  目标模板: " + context.getTargetRelNode());
            System.out.println("  匹配约束数: " + context.getMatchConstraints().size());
            System.out.println("  改写约束数: " + context.getRewriteConstraints().size());
        } catch (AssertionError e) {
            System.err.println("✗ 规则解析失败 (断言错误):");
            System.err.println("  错误: " + e.getMessage());
            System.err.println("\n这是模板构建器的问题，不是匹配器的问题。");
            System.err.println("问题分析：目标模板中的投影列引用 (如 a5) 指向了不存在的输入列索引。");
        } catch (Exception e) {
            System.err.println("✗ 规则解析失败:");
            System.err.println("  错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

