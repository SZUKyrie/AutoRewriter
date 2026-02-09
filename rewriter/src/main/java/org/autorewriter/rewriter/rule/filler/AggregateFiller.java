package org.autorewriter.rewriter.rule.filler;

import lombok.AllArgsConstructor;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalAggregate;
import org.autorewriter.rewriter.rule.RelNodeFiller;

import java.util.Map;
import java.util.function.BiFunction;

@AllArgsConstructor
public class AggregateFiller implements RelNodeFiller<LogicalAggregate> {

    private final BiFunction<RelNode, Map<String, Object>, RelNode> fillTargetTemplateFunc;

    @Override
    public RelNode fill(LogicalAggregate template, Map<String, Object> bindings) {
        RelNode filledInput = fillTargetTemplateFunc.apply(template.getInput(), bindings);

        return LogicalAggregate.create(
            filledInput,
            template.getHints(),
            template.getGroupSet(),
            template.getGroupSets(),
            template.getAggCallList()
        );
    }
}
