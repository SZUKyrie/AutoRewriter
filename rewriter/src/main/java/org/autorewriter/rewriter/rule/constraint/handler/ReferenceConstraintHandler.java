package org.autorewriter.rewriter.rule.constraint.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.autorewriter.rewriter.rule.constraint.BindingResolver;
import org.autorewriter.rewriter.rule.constraint.ConstraintHandler;
import org.autorewriter.rewriter.rule.constraint.ConstraintUtils;
import org.autorewriter.rewriter.rule.util.ColumnRef;

import java.util.Map;
import java.util.Set;

@Slf4j
public class ReferenceConstraintHandler implements ConstraintHandler {

    @Override
    public String getType() {
        return "REFERENCE";
    }

    @Override
    public boolean evaluate(String[] params, Map<String, Object> bindings) {
        if (params.length < 4) {
            log.debug("Reference: insufficient params (need 4, got {})", params.length);
            return true;
        }

        String t0Param = params[0];
        String a0Param = params[1];
        String t1Param = params[2];
        String a1Param = params[3];

        RelNode rel0 = ConstraintUtils.resolveRelNode(bindings.get(t0Param));
        RelNode rel1 = ConstraintUtils.resolveRelNode(bindings.get(t1Param));

        if (rel0 == null || rel1 == null) {
            log.debug("Reference({}, {}, {}, {}): one or both tables not bound", t0Param, a0Param, t1Param, a1Param);
            return true;
        }

        Set<ColumnRef> cols0 = BindingResolver.resolveColRefSet(a0Param, bindings, rel0);
        Set<ColumnRef> cols1 = BindingResolver.resolveColRefSet(a1Param, bindings, rel1);

        if (cols0 == null || cols1 == null) {
            log.debug("Reference({}, {}, {}, {}): cannot resolve columns", t0Param, a0Param, t1Param, a1Param);
            return true;
        }

        boolean matched = cols0.equals(cols1);
        log.debug("Reference({}, {}, {}, {}): {} vs {} = {}", t0Param, a0Param, t1Param, a1Param, cols0, cols1, matched);
        return matched;
    }
}
