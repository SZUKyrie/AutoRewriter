package org.autorewriter.rewriter.analyze;


import org.apache.calcite.rel.RelNode;
import org.apache.shardingsphere.sql.parser.api.ASTNode;
import org.apache.shardingsphere.sqlfederation.autorewriter.RewriteRuleParser;
import org.apache.shardingsphere.sqlfederation.compiler.sql.ast.template.TemplateRelNodeRule;

import java.util.Collection;

/**
 * Analyzer for rewrite rules.
 * Parses rule templates, normalizes placeholders, and converts them to RelNodes.
 * Uses a direct SqlValidator and SqlToRelConverter approach instead of Planner.
 */
public class RuleAnalyzer {
    /**
     * Analyze a rewrite rule string and convert it to RelNodes.
     *
     * @param ruleStr the rule string to analyze
     * @return RuleAnalysisContext containing source/target RelNodes and constraints
     */
    public static RuleAnalysisContext analyze(String ruleStr) {
        RewriteRuleParser rewriteRuleParser = RewriteRuleParser.createParser();
        TemplateRelNodeRule templateRelNodeRule = rewriteRuleParser.parse(ruleStr);
        RelNode sourceRelNode = templateRelNodeRule.getSourceTemplate();
        RelNode targetRelNode = templateRelNodeRule.getTargetTemplate();
        Collection<? extends ASTNode> matchConstraints = templateRelNodeRule.getMatchConstraints();
        Collection<? extends ASTNode> rewriteConstraints = templateRelNodeRule.getRewriteConstraints();

        return new RuleAnalysisContext(
            sourceRelNode,
            targetRelNode,
            matchConstraints,
            rewriteConstraints
        );
    }
}
