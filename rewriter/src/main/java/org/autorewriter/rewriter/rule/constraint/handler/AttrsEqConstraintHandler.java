package org.autorewriter.rewriter.rule.constraint.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rex.RexInputRef;
import org.autorewriter.rewriter.rule.constraint.ConstraintHandler;

import java.util.Map;

@Slf4j
public class AttrsEqConstraintHandler implements ConstraintHandler {

    @Override
    public String getType() {
        return "ATTRS_EQ";
    }

    @Override
    public boolean evaluate(String[] params, Map<String, Object> bindings) {
        String a1 = params[0];
        String a2 = params[1];

        Object idx1 = bindings.get(a1 + "_index");
        Object idx2 = bindings.get(a2 + "_index");

        if (idx1 != null && idx2 != null) {
            boolean eq = idx1.equals(idx2);
            log.debug("AttrsEq({}, {}): index {} vs {} = {}", a1, a2, idx1, idx2, eq);
            return eq;
        }

        Object val1 = bindings.get(a1);
        Object val2 = bindings.get(a2);

        if (val1 != null && val2 != null) {
            if (val1 instanceof RexInputRef && val2 instanceof RexInputRef) {
                RexInputRef ref1 = (RexInputRef) val1;
                RexInputRef ref2 = (RexInputRef) val2;
                boolean eq = ref1.getIndex() == ref2.getIndex();
                log.debug("AttrsEq({}, {}): RexInputRef {} vs {} = {}", a1, a2, ref1.getIndex(), ref2.getIndex(), eq);
                return eq;
            }

            boolean eq = val1.equals(val2);
            log.debug("AttrsEq({}, {}): {} vs {} = {}", a1, a2, val1, val2, eq);
            return eq;
        }

        log.debug("AttrsEq({}, {}): one or both not bound (val1={}, val2={})", a1, a2, val1, val2);
        return false;
    }
}

