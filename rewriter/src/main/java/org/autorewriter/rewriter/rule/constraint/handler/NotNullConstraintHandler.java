package org.autorewriter.rewriter.rule.constraint.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.autorewriter.rewriter.rule.constraint.BindingResolver;
import org.autorewriter.rewriter.rule.constraint.ConstraintHandler;
import org.autorewriter.rewriter.rule.constraint.ConstraintUtils;

import java.util.List;
import java.util.Map;

@Slf4j
public class NotNullConstraintHandler implements ConstraintHandler {

    @Override
    public String getType() {
        return "NOT_NULL";
    }

    @Override
    public boolean evaluate(String[] params, Map<String, Object> bindings) {
        String tableParam = params[0];
        String attrParam = params[1];

        RelNode rel = ConstraintUtils.resolveRelNode(bindings.get(tableParam));
        if (rel == null) {
            log.debug("NotNull({}, {}): not bound to a RelNode, skipping", tableParam, attrParam);
            return true;
        }

        List<Integer> indices = BindingResolver.resolveIndices(attrParam, bindings, rel);
        if (indices == null) {
            log.debug("NotNull({}, {}): column ref not bound, skipping", tableParam, attrParam);
            return true;
        }

        List<RelDataTypeField> fields = rel.getRowType().getFieldList();
        for (int colIdx : indices) {
            if (colIdx >= 0 && colIdx < fields.size() && fields.get(colIdx).getType().isNullable()) {
                log.debug("NotNull({}, {}): col {} is nullable -> false", tableParam, attrParam, colIdx);
                return false;
            }
        }

        log.debug("NotNull({}, {}): all columns not nullable -> true", tableParam, attrParam);
        return true;
    }
}
