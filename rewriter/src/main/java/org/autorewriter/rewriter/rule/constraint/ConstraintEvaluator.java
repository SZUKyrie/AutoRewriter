package org.autorewriter.rewriter.rule.constraint;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalTableScan;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.Table;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.shardingsphere.sql.parser.api.ASTNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConstraintEvaluator {

    public boolean checkMatchConstraints(List<ASTNode> constraints, Map<String, Object> bindings) {
        if (constraints == null || constraints.isEmpty()) {
            return true;
        }

        log.debug("Checking {} match constraints with bindings: {}", constraints.size(), bindings.keySet());

        for (ASTNode constraint : constraints) {
            if (constraint instanceof org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment) {
                org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment cs =
                    (org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment) constraint;

                String constraintType = cs.getType().name();
                String[] params = cs.getParams();

                if (params == null || params.length < 2) {
                    continue;
                }

                boolean satisfied = evaluateMatchConstraint(constraintType, params, bindings);
                if (!satisfied) {
                    log.debug("Constraint {} failed with params: {}", constraintType, Arrays.toString(params));
                    return false;
                }
            }
        }

        return true;
    }

    private boolean evaluateMatchConstraint(String constraintType, String[] params, Map<String, Object> bindings) {
        switch (constraintType) {
            case "PREDICATE_EQ":
                return evaluatePredicateEq(params[0], params[1], bindings);
            case "ATTRS_EQ":
                return evaluateAttrsEq(params[0], params[1], bindings);
            case "ATTRS_SUB":
                return bindings.containsKey(params[0]) || bindings.containsKey(params[0] + "_index");
            case "TABLE_EQ":
                return evaluateTableEq(params[0], params[1], bindings);
            case "UNIQUE":
                return evaluateUnique(params[0], params[1], bindings);
            case "NOT_NULL":
                return evaluateNotNull(params[0], params[1], bindings);
            case "REFERENCE":
                if (params.length < 4) return true;
                return evaluateTableEq(params[0], params[2], bindings)
                        && evaluateAttrsEq(params[1], params[3], bindings);
            default:
                log.debug("Unknown constraint type: {}", constraintType);
                return true;
        }
    }

    private boolean evaluateNotNull(String tableParam, String attrParam, Map<String, Object> bindings) {
        Object tableBinding = bindings.get(tableParam);
        if (!(tableBinding instanceof LogicalTableScan)) {
            log.debug("NotNull({}, {}): table not bound to LogicalTableScan, skipping", tableParam, attrParam);
            return true;
        }
        LogicalTableScan scan = (LogicalTableScan) tableBinding;

        Object idxObj = bindings.get(attrParam + "_index");
        if (!(idxObj instanceof Integer)) {
            log.debug("NotNull({}, {}): column index not bound, skipping", tableParam, attrParam);
            return true;
        }
        int colIdx = (Integer) idxObj;

        // Column nullability is carried directly in the RelOptTable row type
        org.apache.calcite.rel.type.RelDataType rowType = scan.getTable().getRowType();
        List<org.apache.calcite.rel.type.RelDataTypeField> fields = rowType.getFieldList();
        if (colIdx >= fields.size()) {
            log.debug("NotNull({}, {}): column index {} out of range ({})", tableParam, attrParam, colIdx, fields.size());
            return true;
        }

        boolean nullable = fields.get(colIdx).getType().isNullable();
        log.debug("NotNull({}, {}): col index {} nullable={} -> notNull={}", tableParam, attrParam, colIdx, nullable, !nullable);
        return !nullable;
    }

    private boolean evaluateUnique(String tableParam, String attrParam, Map<String, Object> bindings) {
        // tableParam (e.g. t0) is bound to a LogicalTableScan
        Object tableBinding = bindings.get(tableParam);
        if (!(tableBinding instanceof LogicalTableScan)) {
            log.debug("Unique({}, {}): table not bound to LogicalTableScan, skipping", tableParam, attrParam);
            return true;
        }
        LogicalTableScan scan = (LogicalTableScan) tableBinding;

        // attrParam (e.g. a0) index binding holds the 0-based column index in the row type
        Object idxObj = bindings.get(attrParam + "_index");
        if (!(idxObj instanceof Integer)) {
            log.debug("Unique({}, {}): column index not bound, skipping", tableParam, attrParam);
            return true;
        }
        int colIdx = (Integer) idxObj;
        Table schemaTable = scan.getTable().unwrap(Table.class);
        if (schemaTable == null) {
            return true;
        }
        java.util.List<ImmutableBitSet> uniqueKeys = schemaTable.getStatistic().getKeys();
        if (uniqueKeys == null || uniqueKeys.isEmpty()) {
            return false;
        }

        for (org.apache.calcite.util.ImmutableBitSet key : uniqueKeys) {
            if (key.equals(org.apache.calcite.util.ImmutableBitSet.of(colIdx))) {
                return true;
            }
        }

        return false;
    }


    private boolean evaluatePredicateEq(String p1, String p2, Map<String, Object> bindings) {
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

    private boolean evaluateAttrsEq(String a1, String a2, Map<String, Object> bindings) {
        Object idx1 = bindings.get(a1 + "_index");
        Object idx2 = bindings.get(a2 + "_index");

        if (idx1 != null && idx2 != null) {
            boolean eq = idx1.equals(idx2);
            log.debug("AttrsEq({}, {}): index {} vs {} = {}", a1, a2, idx1, idx2, eq);
            return eq;
        }

        Object val1 = bindings.get(a1);
        Object val2 = bindings.get(a2);

        if (val1 != null && val2 != null) {
            if (val1 instanceof RexInputRef && val2 instanceof RexInputRef) {
                RexInputRef ref1 = (RexInputRef) val1;
                RexInputRef ref2 = (RexInputRef) val2;
                boolean eq = ref1.getIndex() == ref2.getIndex();
                log.debug("AttrsEq({}, {}): RexInputRef {} vs {} = {}", a1, a2, ref1.getIndex(), ref2.getIndex(), eq);
                return eq;
            }

            boolean eq = val1.equals(val2);
            log.debug("AttrsEq({}, {}): {} vs {} = {}", a1, a2, val1, val2, eq);
            return eq;
        }

        log.debug("AttrsEq({}, {}): one or both not bound (val1={}, val2={})", a1, a2, val1, val2);
        return false;
    }

    private boolean evaluateTableEq(String t1, String t2, Map<String, Object> bindings) {
        Object node1 = bindings.get(t1);
        Object node2 = bindings.get(t2);

        if (node1 == null || node2 == null) {
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

    public Map<String, Object> applyRewriteConstraints(List<ASTNode> constraints, Map<String, Object> sourceBindings) {
        Map<String, Object> targetBindings = new java.util.HashMap<>(sourceBindings);

        if (constraints == null || constraints.isEmpty()) {
            return targetBindings;
        }

        log.debug("Applying {} rewrite constraints", constraints.size());

        for (ASTNode constraint : constraints) {
            if (constraint instanceof org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment) {
                org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment cs =
                    (org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment) constraint;

                String constraintType = cs.getType().name();
                String[] params = cs.getParams();

                if (params == null || params.length < 2) {
                    continue;
                }

                String targetParam = params[0];
                String sourceParam = params[1];

                if (sourceBindings.containsKey(sourceParam)) {
                    Object value = sourceBindings.get(sourceParam);
                    targetBindings.put(targetParam, value);
                    log.debug("Constraint {}({}, {}) -> {}", constraintType, targetParam, sourceParam,
                        value instanceof RelNode ? ((RelNode)value).getClass().getSimpleName() : value);
                }

                String sourceIndexKey = sourceParam + "_index";
                if (sourceBindings.containsKey(sourceIndexKey)) {
                    Object indexValue = sourceBindings.get(sourceIndexKey);
                    targetBindings.put(targetParam + "_index", indexValue);
                }
            }
        }

        return targetBindings;
    }
}
