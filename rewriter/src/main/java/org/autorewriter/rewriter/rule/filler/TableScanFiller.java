package org.autorewriter.rewriter.rule.filler;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalTableScan;
import org.autorewriter.rewriter.rule.RelNodeFiller;

import java.util.Map;

public class TableScanFiller implements RelNodeFiller<LogicalTableScan> {

    @Override
    public RelNode fill(LogicalTableScan template, Map<String, Object> bindings) {
        String templateTableName = template.getTable().getQualifiedName().get(0);

        if (templateTableName.matches("t\\d+") && bindings.containsKey(templateTableName)) {
            Object boundValue = bindings.get(templateTableName);
            if (boundValue instanceof RelNode) {
                return (RelNode) boundValue;
            }
        }

        return template;
    }
}
