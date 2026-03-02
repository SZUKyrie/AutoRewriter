package org.autorewriter.rewriter.rule.constraint.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rex.RexInputRef;
import org.autorewriter.rewriter.rule.constraint.BindingResolver;
import org.autorewriter.rewriter.rule.constraint.ConstraintHandler;
import org.autorewriter.rewriter.rule.util.ColumnRef;

import java.util.List;
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

        List<ColumnRef> cr1 = BindingResolver.resolveColRefs(a1, bindings);
        List<ColumnRef> cr2 = BindingResolver.resolveColRefs(a2, bindings);

        if (cr1 != null && cr2 != null) {
            boolean eq = colrefMatch(cr1, cr2);
            log.debug("AttrsEq({}, {}): colref {} vs {} = {}", a1, a2, cr1, cr2, eq);
            return eq;
        }

        Object val1 = bindings.get(a1);
        Object val2 = bindings.get(a2);

        if (val1 != null && val2 != null) {
            if (val1 instanceof RexInputRef && val2 instanceof RexInputRef) {
                boolean eq = ((RexInputRef) val1).getIndex() == ((RexInputRef) val2).getIndex();
                log.debug("AttrsEq({}, {}): RexInputRef {} vs {} = {}", a1, a2,
                        ((RexInputRef) val1).getIndex(), ((RexInputRef) val2).getIndex(), eq);
                return eq;
            }

            boolean eq = val1.equals(val2);
            log.debug("AttrsEq({}, {}): {} vs {} = {}", a1, a2, val1, val2, eq);
            return eq;
        }

        log.debug("AttrsEq({}, {}): one or both not bound", a1, a2);
        return false;
    }

    private boolean colrefMatch(List<ColumnRef> cr1, List<ColumnRef> cr2) {
        if (cr1.size() == cr2.size()) {
            return cr1.equals(cr2);
        }
        if (cr1.size() == 1) {
            return cr2.contains(cr1.get(0));
        }
        if (cr2.size() == 1) {
            return cr1.contains(cr2.get(0));
        }
        return cr1.equals(cr2);
    }
}
