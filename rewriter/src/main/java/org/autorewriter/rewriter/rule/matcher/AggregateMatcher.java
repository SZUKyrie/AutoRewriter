package org.autorewriter.rewriter.rule.matcher;

import lombok.AllArgsConstructor;
import org.apache.calcite.rel.logical.LogicalAggregate;
import org.autorewriter.rewriter.rule.RelNodeMatcher;

import java.util.Map;
import java.util.function.BiFunction;

@AllArgsConstructor
public class AggregateMatcher implements RelNodeMatcher<LogicalAggregate> {

    private final BiFunction<org.apache.calcite.rel.RelNode, org.apache.calcite.rel.RelNode, Boolean> recursiveMatchFunc;

    @Override
    public boolean match(LogicalAggregate template, LogicalAggregate query, Map<String, Object> bindings) {
        if (!recursiveMatchFunc.apply(template.getInput(), query.getInput())) {
            return false;
        }

        return template.getAggCallList().isEmpty() && query.getAggCallList().isEmpty();
    }
}
