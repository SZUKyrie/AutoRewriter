package org.autorewriter.rewriter.rule.constraint.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.util.ImmutableBitSet;
import org.autorewriter.rewriter.rule.constraint.BindingResolver;
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

        ImmutableBitSet colBits = BindingResolver.resolveColBits(attrParam, bindings, rel);
        if (colBits == null) {
            log.debug("Unique({}, {}): column ref not resolved, skipping", tableParam, attrParam);
            return true;
        }

        RelMetadataQuery mq = ConstraintUtils.createMetadataQuery();
        Set<ImmutableBitSet> uniqueKeys = mq.getUniqueKeys(rel, false);

        if (uniqueKeys == null || uniqueKeys.isEmpty()) {
            log.debug("Unique({}, {}): no unique keys on {}", tableParam, attrParam, rel.getRelTypeName());
            return false;
        }

        for (ImmutableBitSet key : uniqueKeys) {
            if (colBits.contains(key)) {
                log.debug("Unique({}, {}): cols {} contain unique key {} -> true", tableParam, attrParam, colBits, key);
                return true;
            }
        }

        log.debug("Unique({}, {}): cols {} do not contain any unique key, keys={}", tableParam, attrParam, colBits, uniqueKeys);
        return false;
    }
}
