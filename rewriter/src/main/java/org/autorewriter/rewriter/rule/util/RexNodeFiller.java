package org.autorewriter.rewriter.rule.util;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexSubQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class RexNodeFiller {

    private final BiFunction<RelNode, Map<String, Object>, RelNode> fillRelNodeFunc;

    public RexNodeFiller() {
        this.fillRelNodeFunc = null;
    }

    public RexNodeFiller(BiFunction<RelNode, Map<String, Object>, RelNode> fillRelNodeFunc) {
        this.fillRelNodeFunc = fillRelNodeFunc;
    }

    public RexNode fill(RexNode template, Map<String, Object> bindings, RelNode input) {
        if (template instanceof RexInputRef) {
            RexInputRef templateRef = (RexInputRef) template;
            int templateIndex = templateRef.getIndex();
            String attrName = "a" + templateIndex;

            int actualIndex = resolveActualIndex(attrName, bindings, input);
            if (actualIndex >= 0 && actualIndex < input.getRowType().getFieldCount()) {
                return input.getCluster().getRexBuilder().makeInputRef(
                    input.getRowType().getFieldList().get(actualIndex).getType(),
                    actualIndex
                );
            }

            if (templateIndex < input.getRowType().getFieldCount()) {
                return input.getCluster().getRexBuilder().makeInputRef(
                    input.getRowType().getFieldList().get(templateIndex).getType(),
                    templateIndex
                );
            }

            return template;
        }

        if (template instanceof RexSubQuery && fillRelNodeFunc != null) {
            RexSubQuery templateSub = (RexSubQuery) template;

            List<RexNode> filledOperands = new ArrayList<>();
            for (RexNode operand : templateSub.getOperands()) {
                filledOperands.add(fill(operand, bindings, input));
            }

            RelNode filledRel = fillRelNodeFunc.apply(templateSub.rel, bindings);

            RexSubQuery withOperands = (RexSubQuery) templateSub.clone(
                    templateSub.getType(), filledOperands);
            return withOperands.clone(filledRel);
        }

        if (template instanceof RexCall) {
            RexCall templateCall = (RexCall) template;
            String operatorName = templateCall.getOperator().getName();

            if (operatorName.matches("p\\d+") && bindings.containsKey(operatorName)) {
                return (RexNode) bindings.get(operatorName);
            }

            List<RexNode> filledOperands = new ArrayList<>();
            for (RexNode operand : templateCall.getOperands()) {
                filledOperands.add(fill(operand, bindings, input));
            }

            return templateCall.clone(templateCall.getType(), filledOperands);
        }

        return template;
    }

    public RexNode fillJoinCondition(RexNode template, Map<String, Object> bindings,
                                     RelNode leftInput, RelNode rightInput) {
        if (template instanceof RexInputRef) {
            RexInputRef templateRef = (RexInputRef) template;
            int templateIndex = templateRef.getIndex();
            String attrName = "a" + templateIndex;

            int leftFieldCount = leftInput.getRowType().getFieldCount();

            Object colRefObj = bindings.get(attrName + "_colref");
            if (colRefObj instanceof ColumnRef) {
                ColumnRef colRef = (ColumnRef) colRefObj;
                int leftIdx = ColumnRefResolver.resolveIndex(colRef, leftInput);
                if (leftIdx >= 0) {
                    return leftInput.getCluster().getRexBuilder().makeInputRef(
                        leftInput.getRowType().getFieldList().get(leftIdx).getType(),
                        leftIdx
                    );
                }
                int rightIdx = ColumnRefResolver.resolveIndex(colRef, rightInput);
                if (rightIdx >= 0) {
                    return rightInput.getCluster().getRexBuilder().makeInputRef(
                        rightInput.getRowType().getFieldList().get(rightIdx).getType(),
                        leftFieldCount + rightIdx
                    );
                }
            }

            Object indexVal = bindings.get(attrName + "_index");
            if (indexVal instanceof Integer) {
                int actualIndex = (Integer) indexVal;
                if (actualIndex < leftFieldCount) {
                    return leftInput.getCluster().getRexBuilder().makeInputRef(
                        leftInput.getRowType().getFieldList().get(actualIndex).getType(),
                        actualIndex
                    );
                } else {
                    int rightIndex = actualIndex - leftFieldCount;
                    return rightInput.getCluster().getRexBuilder().makeInputRef(
                        rightInput.getRowType().getFieldList().get(rightIndex).getType(),
                        actualIndex
                    );
                }
            }

            if (templateIndex < leftFieldCount) {
                return leftInput.getCluster().getRexBuilder().makeInputRef(
                    leftInput.getRowType().getFieldList().get(templateIndex).getType(),
                    templateIndex
                );
            } else {
                int rightIndex = templateIndex - leftFieldCount;
                if (rightIndex < rightInput.getRowType().getFieldCount()) {
                    return rightInput.getCluster().getRexBuilder().makeInputRef(
                        rightInput.getRowType().getFieldList().get(rightIndex).getType(),
                        templateIndex
                    );
                }
            }

            return template;
        }

        if (template instanceof RexSubQuery && fillRelNodeFunc != null) {
            RexSubQuery templateSub = (RexSubQuery) template;

            List<RexNode> filledOperands = new ArrayList<>();
            for (RexNode operand : templateSub.getOperands()) {
                filledOperands.add(fillJoinCondition(operand, bindings, leftInput, rightInput));
            }

            RelNode filledRel = fillRelNodeFunc.apply(templateSub.rel, bindings);

            RexSubQuery withOperands = (RexSubQuery) templateSub.clone(
                    templateSub.getType(), filledOperands);
            return withOperands.clone(filledRel);
        }

        if (template instanceof RexCall) {
            RexCall templateCall = (RexCall) template;
            String operatorName = templateCall.getOperator().getName();

            if (operatorName.matches("p\\d+") && bindings.containsKey(operatorName)) {
                return (RexNode) bindings.get(operatorName);
            }

            List<RexNode> filledOperands = new ArrayList<>();
            for (RexNode operand : templateCall.getOperands()) {
                filledOperands.add(fillJoinCondition(operand, bindings, leftInput, rightInput));
            }

            return templateCall.clone(templateCall.getType(), filledOperands);
        }

        return template;
    }

    /**
     * Resolve actual column index from ColumnRef first, then fall back to _index.
     */
    private int resolveActualIndex(String attrName, Map<String, Object> bindings, RelNode input) {
        Object colRefObj = bindings.get(attrName + "_colref");
        if (colRefObj instanceof ColumnRef) {
            int idx = ColumnRefResolver.resolveIndex((ColumnRef) colRefObj, input);
            if (idx >= 0) {
                return idx;
            }
        }
        Object indexVal = bindings.get(attrName + "_index");
        if (indexVal instanceof Integer) {
            return (Integer) indexVal;
        }
        return -1;
    }
}
