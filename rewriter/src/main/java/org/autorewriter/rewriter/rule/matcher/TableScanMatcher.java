package org.autorewriter.rewriter.rule.matcher;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.logical.LogicalTableScan;
import org.apache.calcite.rel.RelNode;
import org.autorewriter.rewriter.rule.RelNodeMatcher;

import java.util.Map;

@Slf4j
public class TableScanMatcher implements RelNodeMatcher<LogicalTableScan> {

    @Override
    public boolean match(LogicalTableScan template, LogicalTableScan query, Map<String, Object> bindings) {
        String templateTableName = template.getTable().getQualifiedName().get(0);

        if (templateTableName.matches("t\\d+")) {
            bindings.put(templateTableName, query);
            log.debug("Bound placeholder {} to RelNode: {}", templateTableName, query.getClass().getSimpleName());
            return true;
        }

        String queryTableName = query.getTable().getQualifiedName().get(0);
        return templateTableName.equals(queryTableName);
    }

    /**
     * Match Input placeholder against any RelNode.
     */
    public boolean matchInputPlaceholder(LogicalTableScan template, RelNode query, Map<String, Object> bindings) {
        String templateTableName = template.getTable().getQualifiedName().get(0);

        if (templateTableName.matches("t\\d+")) {
            bindings.put(templateTableName, query);
            log.debug("Bound placeholder {} to RelNode: {}", templateTableName, query.getClass().getSimpleName());
            return true;
        }

        if (!(query instanceof LogicalTableScan)) {
            return false;
        }

        String queryTableName = ((LogicalTableScan) query).getTable().getQualifiedName().get(0);
        return templateTableName.equals(queryTableName);
    }
}
