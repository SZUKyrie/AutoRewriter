package org.autorewriter.rewriter.rule.util;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RexNodeFiller {

    public RexNode fill(RexNode template, Map<String, Object> bindings, RelNode input) {
        if (template instanceof RexInputRef) {
            RexInputRef templateRef = (RexInputRef) template;
            int templateIndex = templateRef.getIndex();
            String attrName = "a" + templateIndex;

            String indexKey = attrName + "_index";
            if (bindings.containsKey(indexKey)) {
                int actualIndex = (Integer) bindings.get(indexKey);
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

            String indexKey = attrName + "_index";
            if (bindings.containsKey(indexKey)) {
                int actualIndex = (Integer) bindings.get(indexKey);

                int leftFieldCount = leftInput.getRowType().getFieldCount();

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

            int leftFieldCount = leftInput.getRowType().getFieldCount();
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
}
