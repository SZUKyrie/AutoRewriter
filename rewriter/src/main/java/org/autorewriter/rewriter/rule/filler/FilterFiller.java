package org.autorewriter.rewriter.rule.filler;

import lombok.AllArgsConstructor;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rex.RexNode;
import org.autorewriter.rewriter.rule.RelNodeFiller;
import org.autorewriter.rewriter.rule.util.RexNodeFiller;

import java.util.Map;
import java.util.function.BiFunction;

@AllArgsConstructor
public class FilterFiller implements RelNodeFiller<LogicalFilter> {

    private final BiFunction<RelNode, Map<String, Object>, RelNode> fillTargetTemplateFunc;
    private final RexNodeFiller rexNodeFiller;

    @Override
    public RelNode fill(LogicalFilter template, Map<String, Object> bindings) {
        RelNode filledInput = fillTargetTemplateFunc.apply(template.getInput(), bindings);
        RexNode filledCondition = rexNodeFiller.fill(template.getCondition(), bindings, filledInput);

        return LogicalFilter.create(filledInput, filledCondition);
    }
}
