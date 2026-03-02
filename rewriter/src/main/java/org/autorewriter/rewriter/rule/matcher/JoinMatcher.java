package org.autorewriter.rewriter.rule.matcher;

import lombok.AllArgsConstructor;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.logical.LogicalJoin;
import org.apache.calcite.rex.RexNode;
import org.autorewriter.rewriter.rule.RelNodeMatcher;
import org.autorewriter.rewriter.rule.util.RexNodeMatcher;

import java.util.Map;
import java.util.function.BiFunction;

@AllArgsConstructor
public class JoinMatcher implements RelNodeMatcher<LogicalJoin> {

    private final BiFunction<org.apache.calcite.rel.RelNode, org.apache.calcite.rel.RelNode, Boolean> recursiveMatchFunc;
    private final RexNodeMatcher rexNodeMatcher;

    @Override
    public boolean match(LogicalJoin template, LogicalJoin query, Map<String, Object> bindings) {
        if (template.getJoinType() != query.getJoinType()) {
            return false;
        }

        if (!recursiveMatchFunc.apply(template.getLeft(), query.getLeft())) {
            return false;
        }
        if (!recursiveMatchFunc.apply(template.getRight(), query.getRight())) {
            return false;
        }

        return matchJoinCondition(template, query, bindings);
    }

    private boolean matchJoinCondition(LogicalJoin template, LogicalJoin query, Map<String, Object> bindings) {
        RexNode templateCondition = template.getCondition();
        RexNode queryCondition = query.getCondition();

        rexNodeMatcher.setQueryOperator(query);
        return rexNodeMatcher.match(templateCondition, queryCondition, bindings);
    }
}
