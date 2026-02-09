package org.autorewriter.rewriter.rule.filler;

import lombok.AllArgsConstructor;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalJoin;
import org.apache.calcite.rex.RexNode;
import org.autorewriter.rewriter.rule.RelNodeFiller;
import org.autorewriter.rewriter.rule.util.RexNodeFiller;

import java.util.Map;
import java.util.function.BiFunction;

@AllArgsConstructor
public class JoinFiller implements RelNodeFiller<LogicalJoin> {

    private final BiFunction<RelNode, Map<String, Object>, RelNode> fillTargetTemplateFunc;
    private final RexNodeFiller rexNodeFiller;

    @Override
    public RelNode fill(LogicalJoin template, Map<String, Object> bindings) {
        RelNode filledLeft = fillTargetTemplateFunc.apply(template.getLeft(), bindings);
        RelNode filledRight = fillTargetTemplateFunc.apply(template.getRight(), bindings);

        RexNode filledCondition = rexNodeFiller.fillJoinCondition(
            template.getCondition(),
            bindings,
            filledLeft,
            filledRight
        );

        return LogicalJoin.create(
            filledLeft,
            filledRight,
            template.getHints(),
            filledCondition,
            template.getVariablesSet(),
            template.getJoinType()
        );
    }
}
