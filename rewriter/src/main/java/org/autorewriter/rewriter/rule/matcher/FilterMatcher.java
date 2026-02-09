package org.autorewriter.rewriter.rule.matcher;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.autorewriter.rewriter.rule.RelNodeMatcher;
import org.autorewriter.rewriter.rule.util.RexNodeMatcher;

import java.util.Map;
import java.util.function.BiFunction;

@Slf4j
@AllArgsConstructor
public class FilterMatcher implements RelNodeMatcher<LogicalFilter> {

    private final BiFunction<org.apache.calcite.rel.RelNode, org.apache.calcite.rel.RelNode, Boolean> recursiveMatchFunc;
    private final RexNodeMatcher rexNodeMatcher;

    @Override
    public boolean match(LogicalFilter template, LogicalFilter query, Map<String, Object> bindings) {
        log.info("matchFilter: template condition={}, query condition={}",
            template.getCondition(), query.getCondition());

        if (!recursiveMatchFunc.apply(template.getInput(), query.getInput())) {
            log.info("matchFilter: input match failed");
            return false;
        }

        boolean result = rexNodeMatcher.match(template.getCondition(), query.getCondition(), bindings);

        if (result && template.getCondition() instanceof RexCall) {
            extractAndBindAttributes(template.getCondition(), query.getCondition(), bindings);
        }

        return result;
    }

    private void extractAndBindAttributes(RexNode template, RexNode query, Map<String, Object> bindings) {
        if (template instanceof RexInputRef && query instanceof RexInputRef) {
            RexInputRef templateRef = (RexInputRef) template;
            RexInputRef queryRef = (RexInputRef) query;

            String attrName = "a" + templateRef.getIndex();
            bindings.put(attrName, queryRef);
            log.debug("Bound attribute {} to field index {}", attrName, queryRef.getIndex());
        } else if (template instanceof RexCall && query instanceof RexCall) {
            RexCall templateCall = (RexCall) template;
            RexCall queryCall = (RexCall) query;

            for (int i = 0; i < templateCall.getOperands().size() && i < queryCall.getOperands().size(); i++) {
                extractAndBindAttributes(templateCall.getOperands().get(i), queryCall.getOperands().get(i), bindings);
            }
        }
    }
}
