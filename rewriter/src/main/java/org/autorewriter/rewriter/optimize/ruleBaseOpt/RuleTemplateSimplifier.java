package org.autorewriter.rewriter.optimize.ruleBaseOpt;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.hep.HepMatchOrder;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rules.ProjectMergeRule;
import org.apache.calcite.rel.rules.SubQueryRemoveRule;
import org.autorewriter.rewriter.analyze.RuleAnalysisContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Pre-optimize rule templates (source/target RelNodes) before they are registered
 * into the main optimizer. Since rule templates are represented as RelNode trees,
 * standard Calcite simplification rules (e.g. SubQueryRemoveRule, ProjectMergeRule)
 * can be applied to reduce unnecessary nesting.
 */
@Slf4j
public class RuleTemplateSimplifier {

    private final HepPlanner planner;

    public RuleTemplateSimplifier() {
        HepProgramBuilder builder = new HepProgramBuilder();
        builder.addMatchOrder(HepMatchOrder.BOTTOM_UP);
        builder.addRuleInstance(SubQueryRemoveRule.Config.FILTER.toRule());
        builder.addRuleInstance(ProjectMergeRule.Config.DEFAULT.toRule());
        HepProgram program = builder.build();
        this.planner = new HepPlanner(program);
    }

    public List<RuleAnalysisContext> simplify(List<RuleAnalysisContext> contexts) {
        List<RuleAnalysisContext> result = new ArrayList<>(contexts.size());
        for (int i = 0; i < contexts.size(); i++) {
            result.add(simplify(contexts.get(i), i));
        }
        return result;
    }

    private RuleAnalysisContext simplify(RuleAnalysisContext context, int index) {
        RelNode simplifiedSource = simplifyRelNode(context.getSourceRelNode(), "rule[" + index + "].source");
        RelNode simplifiedTarget = simplifyRelNode(context.getTargetRelNode(), "rule[" + index + "].target");

        if (simplifiedSource == context.getSourceRelNode() && simplifiedTarget == context.getTargetRelNode()) {
            return context;
        }

        return new RuleAnalysisContext(
                simplifiedSource,
                simplifiedTarget,
                context.getMatchConstraints(),
                context.getRewriteConstraints()
        );
    }

    private RelNode simplifyRelNode(RelNode relNode, String label) {
        try {
            planner.setRoot(relNode);
            RelNode result = planner.findBestExp();
            if (!result.deepEquals(relNode)) {
                log.info("{} simplified:\n  before: {}\n  after:  {}", label,
                        relNode.explain().replace("\n", " "),
                        result.explain().replace("\n", " "));
            }
            return result;
        } catch (Exception e) {
            log.warn("{} simplification failed, keeping original: {}", label, e.getMessage());
            return relNode;
        }
    }
}

