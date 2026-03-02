package org.autorewriter.rewriter.rule.constraint.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.autorewriter.rewriter.rule.constraint.BindingResolver;
import org.autorewriter.rewriter.rule.constraint.ConstraintHandler;
import org.autorewriter.rewriter.rule.constraint.ConstraintUtils;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.autorewriter.rewriter.rule.util.ColumnRefResolver;

import java.util.List;
import java.util.Map;

@Slf4j
public class AttrsSubConstraintHandler implements ConstraintHandler {

    @Override
    public String getType() {
        return "ATTRS_SUB";
    }

    @Override
    public boolean evaluate(String[] params, Map<String, Object> bindings) {
        String attrParam = params[0];
        String tableParam = params[1];

        RelNode rel = ConstraintUtils.resolveRelNode(bindings.get(tableParam));
        if (rel == null) {
            log.debug("AttrsSub({}, {}): table not bound to RelNode, skipping", attrParam, tableParam);
            return true;
        }

        List<ColumnRef> colRefs = BindingResolver.resolveColRefs(attrParam, bindings, rel);
        if (colRefs == null) {
            boolean bound = bindings.containsKey(attrParam);
            log.debug("AttrsSub({}, {}): fallback, attr bound={}", attrParam, tableParam, bound);
            return bound;
        }

        for (ColumnRef cr : colRefs) {
            int idx = ColumnRefResolver.resolveIndex(cr, rel);
            if (idx < 0) {
                log.debug("AttrsSub({}, {}): column {} not found in {}", attrParam, tableParam, cr, rel.getRelTypeName());
                return false;
            }
        }

        log.debug("AttrsSub({}, {}): all {} columns are subset of {}", attrParam, tableParam, colRefs.size(), rel.getRelTypeName());
        return true;
    }
}

