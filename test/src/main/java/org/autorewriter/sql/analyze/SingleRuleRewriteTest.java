package org.autorewriter.sql.analyze;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.AnsiSqlDialect;
import org.autorewriter.common.enums.ComputeEngine;
import org.autorewriter.rewriter.analyze.RuleAnalysisContext;
import org.autorewriter.rewriter.analyze.RuleAnalyzer;
import org.autorewriter.rewriter.optimize.ruleBaseOpt.RuleBaseOptimizer;
import org.autorewriter.rewriter.rule.AutoRewriteRule;
import org.testng.annotations.Test;

import static org.junit.Assert.*;

public class SingleRuleRewriteTest extends PostgresqlSchemaTestBase{

    private static final String SEPARATOR = "─".repeat(60);
    private static final String DOUBLE_SEPARATOR = "═".repeat(60);

    /**
     * Test 1: Remove DISTINCT on unique column (user_id is unique).
     */
    @Test
    public void testRemoveDistinctOnUniqueColumn() {
        String testName = "Remove DISTINCT on Unique Column (user_id)";
        String ruleStr = "Proj*<a0 s0>(Input<t0>)|Proj<a1 s1>(Input<t1>)|AttrsSub(a0,t0);Unique(t0,a0);TableEq(t1,t0);AttrsEq(a1,a0);SchemaEq(s1,s0)";

        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        assertNotNull(ruleContext);

        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalAggregate.class, RelOptRule.any()),
            ruleContext
        );

        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);

        try {
            String sql = "SELECT DISTINCT user_id FROM test_table";
            AnalysisContext analysisContext = SqlAnalyzer.analyze(sql, ComputeEngine.POSTGRESQL);
            RelNode query = analysisContext.getRelNode();

            RelNode optimized = optimizer.optimize(query);
            assertNotNull(optimized);

            printUnifiedOutput(testName, ruleStr, query, optimized);
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test 2: Merge duplicate filters on the same column.
     */
    @Test
    public void testMergeDuplicateFilters() {
        String testName = "Merge Duplicate Filters";
        String ruleStr = "Filter<p1 a1>(Filter<p0 a0>(Input<t0>))" +
                "|Filter<p2 a2>(Input<t1>)" +
                "|AttrsEq(a0,a1);PredicateEq(p0,p1);AttrsSub(a0,t0);AttrsSub(a1,t0);TableEq(t1,t0);AttrsEq(a2,a0);PredicateEq(p2,p0)";

        String sql = "SELECT * FROM test_table WHERE user_id > 100 AND user_id > 100";
        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        assertNotNull(ruleContext);

        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalFilter.class, RelOptRule.any()),
            ruleContext
        );

        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);

        try {
            AnalysisContext analysisContext = SqlAnalyzer.analyze(sql, ComputeEngine.POSTGRESQL);
            RelNode query = analysisContext.getRelNode();

            RelNode optimized = optimizer.optimize(query);
            assertNotNull(optimized);
            assertEquals("SELECT *\n" +
                    "FROM `test_table`\n" +
                    "WHERE `user_id` > 100", relNodeToSql(optimized).trim());
            assertEquals("LogicalProject(user_id=[$0], name=[$1], pid=[$2], money=[$3], disable=[$4], status=[$5], days=[$6], factor=[$7], word_list=[$8], p_date=[$9])\n" +
                    "  LogicalFilter(condition=[>($0, 100)])\n" +
                    "    LogicalTableScan(table=[[test_table]])", optimized.explain().trim());
            //printUnifiedOutput(testName, ruleStr, query, optimized);
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test 6: Filter with boolean column.
     */
    @Test
    public void testFilterWithBooleanColumn() {
        String testName = "Filter with Boolean Column";
        String ruleStr = "Filter<p0 a0>(Input<t0>)|Input<t1>|TableEq(t1,t0);AttrsSub(a0,t0)";

        RuleAnalysisContext ruleContext = RuleAnalyzer.analyze(ruleStr);
        assertNotNull(ruleContext);

        AutoRewriteRule rule = new AutoRewriteRule(
            RelOptRule.operand(org.apache.calcite.rel.logical.LogicalFilter.class, RelOptRule.any()),
            ruleContext
        );

        RuleBaseOptimizer optimizer = new RuleBaseOptimizer();
        optimizer.addRule(rule);

        try {
            String sql = "SELECT user_id, name FROM test_table WHERE disable = true";
            AnalysisContext analysisContext = SqlAnalyzer.analyze(sql, ComputeEngine.POSTGRESQL);
            RelNode query = analysisContext.getRelNode();

            RelNode optimized = optimizer.optimize(query);
            assertNotNull(optimized);

            printUnifiedOutput(testName, ruleStr, query, optimized);
        } catch (Exception e) {
            System.out.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Unified output helper method.
     */
    private void printUnifiedOutput(String testName, String ruleStr, RelNode original, RelNode optimized) {
        System.out.println();
        System.out.println(DOUBLE_SEPARATOR);
        System.out.println("TEST: " + testName);
        System.out.println(DOUBLE_SEPARATOR);

        System.out.println("\n[RULE]");
        System.out.println(ruleStr);

        System.out.println("\n" + SEPARATOR);
        System.out.println("[SOURCE SQL]");
        System.out.println(SEPARATOR);
        System.out.println(relNodeToSql(original));

        System.out.println("\n" + SEPARATOR);
        System.out.println("[SOURCE RELNODE]");
        System.out.println(SEPARATOR);
        System.out.println(original.explain().trim());

        System.out.println("\n" + SEPARATOR);
        System.out.println("[REWRITTEN SQL]");
        System.out.println(SEPARATOR);
        System.out.println(relNodeToSql(optimized));

        System.out.println("\n" + SEPARATOR);
        System.out.println("[REWRITTEN RELNODE]");
        System.out.println(SEPARATOR);
        System.out.println(optimized.explain().trim());

        System.out.println("\n" + SEPARATOR);
        boolean changed = !original.explain().equals(optimized.explain());
        System.out.println("[STATUS] " + (changed ? "✓ Query Rewritten" : "✗ No Change"));
        System.out.println(DOUBLE_SEPARATOR);
    }

    /**
     * Helper method to convert RelNode to SQL string.
     */
    private String relNodeToSql(RelNode relNode) {
        try {
            SqlDialect dialect = AnsiSqlDialect.DEFAULT;
            RelToSqlConverter converter =
                    new RelToSqlConverter(dialect);
            RelToSqlConverter.Result result =
                    converter.visitRoot(relNode);
            SqlNode sqlNode = result.asStatement();
            return sqlNode.toSqlString(dialect).getSql();
        } catch (Exception e) {
            return "Failed to convert to SQL: " + e.getMessage();
        }
    }
}
