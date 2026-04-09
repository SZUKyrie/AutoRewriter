package e2e;

import org.autorewriter.e2e.RewritePathE2ETest;
import org.autorewriter.rewriter.optimize.OptimizeResult;
import org.autorewriter.rewriter.pipleline.result.ProduceResult;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AutoRwFakeE2ERuleGroupTest extends RewritePathE2ETest {
    @Test
    public void testDiasporaWithRuleGroup1() {
        List<String> rules = new ArrayList<>();
        rules.add("InSubFilter<a3>(Input<t0>,Proj<a2 s1>(InSubFilter<a1>(Input<t1>,Proj<a0 s0>(Input<t2>))))|InSubFilter<a7>(Input<t3>,Proj<a6 s2>(InnerJoin<a4 a5>(Input<t4>,Input<t5>)))|AttrsSub(a0,t2);AttrsSub(a1,t1);AttrsSub(a2,t1);AttrsSub(a3,t0);TableEq(t3,t0);TableEq(t4,t1);TableEq(t5,t2);AttrsEq(a4,a1);AttrsEq(a5,a0);AttrsEq(a6,a2);AttrsEq(a7,a3);SchemaEq(s2,s1)");
        rules.add("Proj<a4 s1>(InnerJoin<a2 a3>(Proj<a1 s0>(Filter<p0 a0>(Input<t0>)),Input<t1>))|Proj<a8 s2>(Filter<p1 a7>(InnerJoin<a5 a6>(Input<t2>,Input<t3>)))|AttrsSub(a0,t0);AttrsSub(a1,t0);AttrsSub(a2,s0);AttrsSub(a3,t1);AttrsSub(a4,s0);TableEq(t2,t0);TableEq(t3,t1);AttrsEq(a5,a2);AttrsEq(a6,a3);AttrsEq(a7,a0);AttrsEq(a8,a4);PredicateEq(p1,p0);SchemaEq(s2,s1)");
        rules.add("InnerJoin<a0 a2>(InnerJoin<a0 a1>(Input<t0>,Input<t1>),Input<t2>)|InnerJoin<a1 a5>(InnerJoin<a3 a4>(Input<t3>,Input<t4>),Input<t5>)|AttrsEq(a0,a1);AttrsSub(a0,t1);AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t2);TableEq(t3,t0);TableEq(t4,t1);TableEq(t5,t2);AttrsEq(a3,a0);AttrsEq(a4,a1);AttrsEq(a1,a0);AttrsEq(a5,a2)");
        rules.add("InnerJoin<a0 a2>(InnerJoin<a0 a1>(Input<t0>,Input<t1>),Input<t2>)|InnerJoin<a1 a5>(InnerJoin<a3 a4>(Input<t3>,Input<t4>),Input<t5>)|AttrsEq(a0,a0);AttrsSub(a0,t0);AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t2);TableEq(t3,t0);TableEq(t4,t1);TableEq(t5,t2);AttrsEq(a3,a0);AttrsEq(a4,a1);AttrsEq(a1,a1);AttrsEq(a5,a2)");

        ProduceResult result = executePipeline(PipelineType.MANUAL, "diaspora", rules);
        OptimizeResult opt = result.getOptimizeResults().get(0);
        assertNotNull("Optimized RelNode should not be null", opt.getOptimizedRelNode());
        assertEquals("LogicalSort(offset=[0], fetch=[15])\n" +
                        "  LogicalInSubFilter(lhsRef=[$0])\n" +
                        "    LogicalTableScan(table=[[people]])\n" +
                        "    LogicalProject(id=[$0])\n" +
                        "      LogicalFilter(condition=[AND(=($18, 322), =($11, 488))])\n" +
                        "        LogicalJoin(condition=[=($0, $22)], joinType=[inner])\n" +
                        "          LogicalJoin(condition=[=($12, $19)], joinType=[inner])\n" +
                        "            LogicalJoin(condition=[=($12, $0)], joinType=[inner])\n" +
                        "              LogicalTableScan(table=[[people]])\n" +
                        "              LogicalTableScan(table=[[contacts]])\n" +
                        "            LogicalTableScan(table=[[aspect_memberships]])\n" +
                        "          LogicalFilter(condition=[AND(OR(=($20, true), =($41, 488)), OR(LIKE($25, _UTF-8'%my% aspect% contact%'), LIKE($2, _UTF-8'myaspectcontact%')), =($7, false), =($41, 488), =($36, 321))])\n" +
                        "            LogicalJoin(condition=[AND(=($41, 488), =($42, $0))], joinType=[left])\n" +
                        "              LogicalJoin(condition=[=($30, $37)], joinType=[inner])\n" +
                        "                LogicalJoin(condition=[=($0, $30)], joinType=[inner])\n" +
                        "                  LogicalJoin(condition=[=($21, $0)], joinType=[inner])\n" +
                        "                    LogicalTableScan(table=[[people]])\n" +
                        "                    LogicalTableScan(table=[[profiles]])\n" +
                        "                  LogicalTableScan(table=[[contacts]])\n" +
                        "                LogicalTableScan(table=[[aspect_memberships]])\n" +
                        "              LogicalTableScan(table=[[contacts]])\n",
                opt.getOptimizedRelNode().explain());
    }

    @Test
    public void testDiasporaWithRuleGroup2() {
        List<String> rules = new ArrayList<>();
        rules.add("InSubFilter<a3>(Input<t0>,Proj<a2 s1>(InSubFilter<a1>(Input<t1>,Proj<a0 s0>(Input<t2>))))|InSubFilter<a7>(Input<t3>,Proj<a6 s2>(InnerJoin<a4 a5>(Input<t4>,Input<t5>)))|AttrsSub(a0,t2);AttrsSub(a1,t1);AttrsSub(a2,t1);AttrsSub(a3,t0);TableEq(t3,t0);TableEq(t4,t2);TableEq(t5,t1);AttrsEq(a4,a0);AttrsEq(a5,a1);AttrsEq(a6,a2);AttrsEq(a7,a3);SchemaEq(s2,s1)");
        rules.add("Proj<a4 s1>(InnerJoin<a2 a3>(Proj<a1 s0>(Filter<p0 a0>(Input<t0>)),Input<t1>))|Proj<a8 s2>(Filter<p1 a7>(InnerJoin<a5 a6>(Input<t2>,Input<t3>)))|AttrsSub(a0,t0);AttrsSub(a1,t0);AttrsSub(a2,s0);AttrsSub(a3,t1);AttrsSub(a4,t1);TableEq(t2,t0);TableEq(t3,t1);AttrsEq(a5,a2);AttrsEq(a6,a3);AttrsEq(a7,a0);AttrsEq(a8,a4);PredicateEq(p1,p0);SchemaEq(s2,s1)");
        rules.add("Proj<a5 s1>(Filter<p1 a4>(InnerJoin<a2 a3>(Input<t0>,Proj<a1 s0>(Filter<p0 a0>(Input<t1>)))))|Proj<a10 s2>(Filter<p3 a9>(Filter<p2 a8>(InnerJoin<a6 a7>(Input<t2>,Input<t3>))))|AttrsSub(a0,t1);AttrsSub(a1,t1);AttrsSub(a2,t0);AttrsSub(a3,s0);AttrsSub(a4,t0);AttrsSub(a5,s0);TableEq(t2,t0);TableEq(t3,t1);AttrsEq(a6,a2);AttrsEq(a7,a3);AttrsEq(a8,a0);AttrsEq(a9,a4);AttrsEq(a10,a5);PredicateEq(p2,p0);PredicateEq(p3,p1);SchemaEq(s2,s1)");
        rules.add("InnerJoin<a0 a2>(InnerJoin<a0 a1>(Input<t0>,Input<t1>),Input<t2>)|InnerJoin<a1 a5>(InnerJoin<a3 a4>(Input<t3>,Input<t4>),Input<t5>)|AttrsEq(a0,a0);AttrsSub(a0,t0);AttrsSub(a0,t0);AttrsSub(a1,t1);AttrsSub(a2,t2);TableEq(t3,t0);TableEq(t4,t1);TableEq(t5,t2);AttrsEq(a3,a0);AttrsEq(a4,a1);AttrsEq(a1,a1);AttrsEq(a5,a2)");


        ProduceResult result = executePipeline(PipelineType.MANUAL, "diaspora", rules);
        OptimizeResult opt = result.getOptimizeResults().get(0);
        assertNotNull("Optimized RelNode should not be null", opt.getOptimizedRelNode());
        assertEquals("LogicalSort(offset=[0], fetch=[15])\n" +
                        "  LogicalInSubFilter(lhsRef=[$0])\n" +
                        "    LogicalTableScan(table=[[people]])\n" +
                        "    LogicalProject(id=[$0])\n" +
                        "      LogicalFilter(condition=[AND(OR(=($20, true), =($41, 488)), OR(LIKE($25, _UTF-8'%my% aspect% contact%'), LIKE($2, _UTF-8'myaspectcontact%')), =($7, false), =($41, 488), =($36, 321), =($36, 322), =($29, 488))])\n" +
                        "        LogicalJoin(condition=[=($30, $66)], joinType=[inner])\n" +
                        "          LogicalJoin(condition=[=($28, $59)], joinType=[inner])\n" +
                        "            LogicalJoin(condition=[=($0, $47)], joinType=[inner])\n" +
                        "              LogicalJoin(condition=[AND(=($41, 488), =($42, $0))], joinType=[left])\n" +
                        "                LogicalJoin(condition=[=($30, $37)], joinType=[inner])\n" +
                        "                  LogicalJoin(condition=[=($0, $30)], joinType=[inner])\n" +
                        "                    LogicalJoin(condition=[=($21, $0)], joinType=[inner])\n" +
                        "                      LogicalTableScan(table=[[people]])\n" +
                        "                      LogicalTableScan(table=[[profiles]])\n" +
                        "                    LogicalTableScan(table=[[contacts]])\n" +
                        "                  LogicalTableScan(table=[[aspect_memberships]])\n" +
                        "                LogicalTableScan(table=[[contacts]])\n" +
                        "              LogicalTableScan(table=[[people]])\n" +
                        "            LogicalTableScan(table=[[contacts]])\n" +
                        "          LogicalTableScan(table=[[aspect_memberships]])\n",
                opt.getOptimizedRelNode().explain());
    }

    @Test
    public void testDiasporaWithRuleGroup3() {
        List<String> rules = new ArrayList<>();
        rules.add("InSubFilter<a3>(Input<t0>,Proj<a2 s1>(InSubFilter<a1>(Input<t1>,Proj<a0 s0>(Input<t2>))))|InSubFilter<a7>(Input<t3>,Proj<a6 s2>(InnerJoin<a4 a5>(Input<t4>,Input<t5>)))|AttrsSub(a0,t2);AttrsSub(a1,t1);AttrsSub(a2,t1);AttrsSub(a3,t0);TableEq(t3,t0);TableEq(t4,t2);TableEq(t5,t1);AttrsEq(a4,a0);AttrsEq(a5,a1);AttrsEq(a6,a2);AttrsEq(a7,a3);SchemaEq(s2,s1)");
        rules.add("Proj<a4 s1>(InnerJoin<a2 a3>(Proj<a1 s0>(Filter<p0 a0>(Input<t0>)),Input<t1>))|Proj<a8 s2>(Filter<p1 a7>(InnerJoin<a5 a6>(Input<t2>,Input<t3>)))|AttrsSub(a0,t0);AttrsSub(a1,t0);AttrsSub(a2,s0);AttrsSub(a3,t1);AttrsSub(a4,t1);TableEq(t2,t0);TableEq(t3,t1);AttrsEq(a5,a2);AttrsEq(a6,a3);AttrsEq(a7,a0);AttrsEq(a8,a4);PredicateEq(p1,p0);SchemaEq(s2,s1)");
        rules.add("Proj<a5 s1>(Filter<p1 a4>(InnerJoin<a2 a3>(Input<t0>,Proj<a1 s0>(Filter<p0 a0>(Input<t1>)))))|Proj<a10 s2>(Filter<p3 a9>(Filter<p2 a8>(InnerJoin<a6 a7>(Input<t2>,Input<t3>))))|AttrsSub(a0,t1);AttrsSub(a1,t1);AttrsSub(a2,t0);AttrsSub(a3,s0);AttrsSub(a4,t0);AttrsSub(a5,t0);TableEq(t2,t0);TableEq(t3,t1);AttrsEq(a6,a2);AttrsEq(a7,a3);AttrsEq(a8,a0);AttrsEq(a9,a4);AttrsEq(a10,a5);PredicateEq(p2,p0);PredicateEq(p3,p1);SchemaEq(s2,s1)");
        rules.add("Filter<p1 a1>(Filter<p0 a0>(Input<t0>))|Filter<p2 a2>(Input<t1>)|AttrsEq(a0,a1);PredicateEq(p0,p1);AttrsSub(a0,t0);AttrsSub(a1,t0);TableEq(t1,t0);AttrsEq(a2,a0);PredicateEq(p2,p0)");


        ProduceResult result = executePipeline(PipelineType.MANUAL, "diaspora", rules);
        OptimizeResult opt = result.getOptimizeResults().get(0);
        assertNotNull("Optimized RelNode should not be null", opt.getOptimizedRelNode());
        assertEquals("LogicalSort(offset=[0], fetch=[15])\n" +
                        "  LogicalInSubFilter(lhsRef=[$0])\n" +
                        "    LogicalTableScan(table=[[people]])\n" +
                        "    LogicalProject(id=[$0])\n" +
                        "      LogicalFilter(condition=[AND(OR(=($20, true), =($41, 488)), OR(LIKE($25, _UTF-8'%my% aspect% contact%'), LIKE($2, _UTF-8'myaspectcontact%')), =($7, false), =($29, 488))])\n" +
                        "        LogicalJoin(condition=[=($28, $66)], joinType=[inner])\n" +
                        "          LogicalJoin(condition=[=($0, $59)], joinType=[inner])\n" +
                        "            LogicalJoin(condition=[=($0, $47)], joinType=[inner])\n" +
                        "              LogicalJoin(condition=[AND(=($41, 488), =($42, $0))], joinType=[left])\n" +
                        "                LogicalJoin(condition=[=($37, $28)], joinType=[inner])\n" +
                        "                  LogicalJoin(condition=[=($30, $0)], joinType=[inner])\n" +
                        "                    LogicalJoin(condition=[=($21, $0)], joinType=[inner])\n" +
                        "                      LogicalTableScan(table=[[people]])\n" +
                        "                      LogicalTableScan(table=[[profiles]])\n" +
                        "                    LogicalTableScan(table=[[contacts]])\n" +
                        "                  LogicalTableScan(table=[[aspect_memberships]])\n" +
                        "                LogicalTableScan(table=[[contacts]])\n" +
                        "              LogicalTableScan(table=[[people]])\n" +
                        "            LogicalTableScan(table=[[contacts]])\n" +
                        "          LogicalTableScan(table=[[aspect_memberships]])\n",
                opt.getOptimizedRelNode().explain());
    }
}
