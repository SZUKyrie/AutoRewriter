package org.autorewriter.rewriter.rule.constraint.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rex.RexNode;
import org.autorewriter.rewriter.rule.constraint.ConstraintHandler;

import java.util.Map;

@Slf4j
public class PredicateEqConstraintHandler implements ConstraintHandler {

    @Override
    public String getType() {
        return "PREDICATE_EQ";
    }

    @Override
    public boolean evaluate(String[] params, Map<String, Object> bindings) {
        String p1 = params[0];
        String p2 = params[1];

        Object pred1 = bindings.get(p1);
        Object pred2 = bindings.get(p2);

        if (pred1 == null || pred2 == null) {
            log.debug("PredicateEq: {} or {} not bound", p1, p2);
            return false;
        }

        if (pred1 instanceof RexNode && pred2 instanceof RexNode) {
            String str1 = pred1.toString();
            String str2 = pred2.toString();
            boolean eq = str1.equals(str2);
            log.debug("PredicateEq({}, {}): {} vs {} = {}", p1, p2, str1, str2, eq);
            return eq;
        }

        return pred1.equals(pred2);
    }
}

