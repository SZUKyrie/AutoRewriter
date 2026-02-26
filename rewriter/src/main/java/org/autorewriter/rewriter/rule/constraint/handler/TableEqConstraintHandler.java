package org.autorewriter.rewriter.rule.constraint.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalTableScan;
import org.autorewriter.rewriter.rule.constraint.ConstraintHandler;

import java.util.Map;

@Slf4j
public class TableEqConstraintHandler implements ConstraintHandler {

    @Override
    public String getType() {
        return "TABLE_EQ";
    }

    @Override
    public boolean evaluate(String[] params, Map<String, Object> bindings) {
        String t1 = params[0];
        String t2 = params[1];

        Object node1 = bindings.get(t1);
        Object node2 = bindings.get(t2);

        if (node1 == null || node2 == null) {
            log.debug("TableEq({}, {}): one or both not bound", t1, t2);
            return false;
        }

        if (node1 instanceof RelNode && node2 instanceof RelNode) {
            return compareRelNodes((RelNode) node1, (RelNode) node2);
        }

        return node1.equals(node2);
    }

    private boolean compareRelNodes(RelNode node1, RelNode node2) {
        if (node1.getClass() != node2.getClass()) {
            return false;
        }

        if (node1 instanceof LogicalTableScan && node2 instanceof LogicalTableScan) {
            String name1 = ((LogicalTableScan) node1).getTable().getQualifiedName().toString();
            String name2 = ((LogicalTableScan) node2).getTable().getQualifiedName().toString();
            return name1.equals(name2);
        }

        return node1.deepEquals(node2);
    }
}

