package org.autorewriter.rewriter.rule.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexSubQuery;

import java.util.Map;
import java.util.function.BiFunction;

@Slf4j
public class RexNodeMatcher {

    /**
     * 用于递归匹配子查询内部 RelNode 树，由 AutoRewriteRule 注入。
     * 若为 null，则遇到 RexSubQuery 时退化为 template.equals(query)。
     */
    private final BiFunction<RelNode, RelNode, Boolean> recursiveMatchFunc;

    public RexNodeMatcher() {
        this.recursiveMatchFunc = null;
    }

    public RexNodeMatcher(BiFunction<RelNode, RelNode, Boolean> recursiveMatchFunc) {
        this.recursiveMatchFunc = recursiveMatchFunc;
    }

    public boolean match(RexNode template, RexNode query, Map<String, Object> bindings) {
        if (template instanceof RexInputRef && query instanceof RexInputRef) {
            RexInputRef templateRef = (RexInputRef) template;
            RexInputRef queryRef = (RexInputRef) query;

            String attrName = "a" + templateRef.getIndex();
            bindings.put(attrName + "_index", queryRef.getIndex());

            return true;
        }

        // RexSubQuery 是 RexCall 的子类，需要在 RexCall 分支之前处理
        if (template instanceof RexSubQuery && query instanceof RexSubQuery) {
            RexSubQuery templateSub = (RexSubQuery) template;
            RexSubQuery querySub = (RexSubQuery) query;

            // 操作符必须相同（IN / EXISTS / SCALAR / UNIQUE）
            if (!templateSub.getOperator().equals(querySub.getOperator())) {
                return false;
            }

            // 匹配子查询外部的 RexNode operands（如 IN($0, ...) 中的 $0）
            if (templateSub.getOperands().size() != querySub.getOperands().size()) {
                return false;
            }
            for (int i = 0; i < templateSub.getOperands().size(); i++) {
                if (!match(templateSub.getOperands().get(i), querySub.getOperands().get(i), bindings)) {
                    return false;
                }
            }

            // 递归匹配子查询内部的 RelNode 树
            if (recursiveMatchFunc != null) {
                return recursiveMatchFunc.apply(templateSub.rel, querySub.rel);
            }
            // 没有注入 recursiveMatchFunc 时，只要 operator 和 operands 一致即认为匹配
            return true;
        }

        if (template instanceof RexCall && query instanceof RexCall) {
            RexCall templateCall = (RexCall) template;
            RexCall queryCall = (RexCall) query;

            String operatorName = templateCall.getOperator().getName();
            if (operatorName.matches("p\\d+")) {
                bindings.put(operatorName, queryCall);
                return true;
            }

            if (!templateCall.getOperator().equals(queryCall.getOperator())) {
                return false;
            }

            if (templateCall.getOperands().size() != queryCall.getOperands().size()) {
                return false;
            }

            for (int i = 0; i < templateCall.getOperands().size(); i++) {
                if (!match(templateCall.getOperands().get(i), queryCall.getOperands().get(i), bindings)) {
                    return false;
                }
            }

            return true;
        }

        return template.equals(query);
    }
}
