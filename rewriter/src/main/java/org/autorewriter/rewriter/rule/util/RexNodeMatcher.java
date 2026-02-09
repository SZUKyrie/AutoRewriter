package org.autorewriter.rewriter.rule.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;

import java.util.Map;

@Slf4j
public class RexNodeMatcher {

    public boolean match(RexNode template, RexNode query, Map<String, Object> bindings) {
        if (template instanceof RexInputRef && query instanceof RexInputRef) {
            RexInputRef templateRef = (RexInputRef) template;
            RexInputRef queryRef = (RexInputRef) query;

            String attrName = "a" + templateRef.getIndex();
            bindings.put(attrName + "_index", queryRef.getIndex());

            return true;
        }

        if (template instanceof RexCall && query instanceof RexCall) {
            RexCall templateCall = (RexCall) template;
            RexCall queryCall = (RexCall) query;

            String operatorName = templateCall.getOperator().getName();
            if (operatorName.matches("p\\d+")) {
                bindings.put(operatorName, queryCall);
                return true;
            }

            if (!templateCall.getOperator().equals(queryCall.getOperator())) {
                return false;
            }

            if (templateCall.getOperands().size() != queryCall.getOperands().size()) {
                return false;
            }

            for (int i = 0; i < templateCall.getOperands().size(); i++) {
                if (!match(templateCall.getOperands().get(i), queryCall.getOperands().get(i), bindings)) {
                    return false;
                }
            }

            return true;
        }

        return template.equals(query);
    }
}
