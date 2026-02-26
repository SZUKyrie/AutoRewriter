package org.autorewriter.rewriter.rule.constraint.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.util.ImmutableBitSet;
import org.autorewriter.rewriter.rule.constraint.ConstraintHandler;
import org.autorewriter.rewriter.rule.constraint.ConstraintUtils;

import java.util.Map;
import java.util.Set;

@Slf4j
public class UniqueConstraintHandler implements ConstraintHandler {

    @Override
    public String getType() {
        return "UNIQUE";
    }

    @Override
    public boolean evaluate(String[] params, Map<String, Object> bindings) {
        String tableParam = params[0];
        String attrParam = params[1];

        RelNode rel = ConstraintUtils.resolveRelNode(bindings.get(tableParam));
        if (rel == null) {
            log.debug("Unique({}, {}): not bound to a RelNode, skipping", tableParam, attrParam);
            return true;
        }

        Integer colIdx = ConstraintUtils.resolveColIndex(attrParam, bindings);
        if (colIdx == null) {
            log.debug("Unique({}, {}): column index not bound, skipping", tableParam, attrParam);
            return true;
        }

        RelMetadataQuery mq = ConstraintUtils.createMetadataQuery();
        Set<ImmutableBitSet> uniqueKeys = mq.getUniqueKeys(rel, false);

        if (uniqueKeys == null || uniqueKeys.isEmpty()) {
            log.debug("Unique({}, {}): no unique keys on {}", tableParam, attrParam, rel.getRelTypeName());
            return false;
        }

        ImmutableBitSet colBit = ImmutableBitSet.of(colIdx);
        for (ImmutableBitSet key : uniqueKeys) {
            if (key.equals(colBit)) {
                log.debug("Unique({}, {}): col {} matched unique key -> true", tableParam, attrParam, colIdx);
                return true;
            }
        }

        log.debug("Unique({}, {}): col {} not a sole unique key, keys={}", tableParam, attrParam, colIdx, uniqueKeys);
        return false;
    }
}

