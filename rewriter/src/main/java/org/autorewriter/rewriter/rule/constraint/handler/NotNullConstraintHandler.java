package org.autorewriter.rewriter.rule.constraint.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataTypeField;
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

        Integer colIdx = ConstraintUtils.resolveColIndex(attrParam, bindings);
        if (colIdx == null) {
            log.debug("NotNull({}, {}): column index not bound, skipping", tableParam, attrParam);
            return true;
        }

        List<RelDataTypeField> fields = rel.getRowType().getFieldList();
        if (colIdx >= fields.size()) {
            log.debug("NotNull({}, {}): col index {} out of range ({})", tableParam, attrParam, colIdx, fields.size());
            return true;
        }

        boolean nullable = fields.get(colIdx).getType().isNullable();
        log.debug("NotNull({}, {}): col {} nullable={} -> notNull={}", tableParam, attrParam, colIdx, nullable, !nullable);
        return !nullable;
    }
}

