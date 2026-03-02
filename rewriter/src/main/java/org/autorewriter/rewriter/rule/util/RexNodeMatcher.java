package org.autorewriter.rewriter.rule.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexSubQuery;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

@Slf4j
public class RexNodeMatcher {

    private final BiFunction<RelNode, RelNode, Boolean> recursiveMatchFunc;

    /**
     * The query operator whose input row type gives meaning to RexInputRef indices.
     * Must be set via {@link #setQueryOperator} before calling {@link #match}.
     */
    private RelNode queryOperator;

    public RexNodeMatcher() {
        this.recursiveMatchFunc = null;
    }

    public RexNodeMatcher(BiFunction<RelNode, RelNode, Boolean> recursiveMatchFunc) {
        this.recursiveMatchFunc = recursiveMatchFunc;
    }

    public void setQueryOperator(RelNode queryOperator) {
        this.queryOperator = queryOperator;
    }

    public boolean match(RexNode template, RexNode query, Map<String, Object> bindings) {
        if (template instanceof RexInputRef && query instanceof RexInputRef) {
            RexInputRef templateRef = (RexInputRef) template;
            RexInputRef queryRef = (RexInputRef) query;

            String attrName = "a" + templateRef.getIndex();

            if (queryOperator != null && !bindings.containsKey(attrName + "_colref")) {
                ColumnRef colRef = ColumnRefResolver.resolve(queryRef.getIndex(), queryOperator);
                bindings.put(attrName + "_colref", colRef);
            }
            if (!bindings.containsKey(attrName + "_index")) {
                bindings.put(attrName + "_index", queryRef.getIndex());
            }

            return true;
        }

        if (template instanceof RexSubQuery && query instanceof RexSubQuery) {
            RexSubQuery templateSub = (RexSubQuery) template;
            RexSubQuery querySub = (RexSubQuery) query;

            if (!templateSub.getOperator().equals(querySub.getOperator())) {
                return false;
            }

            if (templateSub.getOperands().size() != querySub.getOperands().size()) {
                return false;
            }
            for (int i = 0; i < templateSub.getOperands().size(); i++) {
                if (!match(templateSub.getOperands().get(i), querySub.getOperands().get(i), bindings)) {
                    return false;
                }
            }

            if (recursiveMatchFunc != null) {
                return recursiveMatchFunc.apply(templateSub.rel, querySub.rel);
            }
            return true;
        }

        if (template instanceof RexCall && query instanceof RexCall) {
            RexCall templateCall = (RexCall) template;
            RexCall queryCall = (RexCall) query;

            String operatorName = templateCall.getOperator().getName();
            if (operatorName.matches("p\\d+")) {
                bindings.put(operatorName, queryCall);
                bindPredicateAttributes(templateCall, queryCall, bindings);
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

        if (template instanceof RexInputRef && query instanceof RexCall) {
            RexInputRef templateRef = (RexInputRef) template;
            String attrName = "a" + templateRef.getIndex();
            List<ColumnRef> allColRefs = new ArrayList<>();
            List<Integer> allIndices = new ArrayList<>();
            collectAllRexInputRefs(query, allColRefs, allIndices);
            if (!allColRefs.isEmpty() && !bindings.containsKey(attrName + "_colref")) {
                bindings.put(attrName + "_colref", allColRefs);
            }
            if (!allIndices.isEmpty() && !bindings.containsKey(attrName + "_index")) {
                bindings.put(attrName + "_index", allIndices);
            }
            return true;
        }

        return template.equals(query);
    }

    /**
     * When a predicate placeholder p0 matches the entire query condition,
     * bind each template operand (which should be a RexInputRef like $0)
     * to ALL RexInputRefs found in the query condition.
     * This supports templates like p0($0) matching AND(=($11,488),=($18,322))
     * where a0 should represent all columns referenced by the predicate.
     */
    private void bindPredicateAttributes(RexCall templateCall, RexCall queryCall, Map<String, Object> bindings) {
        List<ColumnRef> allColRefs = new ArrayList<>();
        List<Integer> allIndices = new ArrayList<>();
        collectAllRexInputRefs(queryCall, allColRefs, allIndices);

        for (RexNode operand : templateCall.getOperands()) {
            if (operand instanceof RexInputRef) {
                RexInputRef templateRef = (RexInputRef) operand;
                String attrName = "a" + templateRef.getIndex();
                if (!allColRefs.isEmpty() && !bindings.containsKey(attrName + "_colref")) {
                    bindings.put(attrName + "_colref", allColRefs);
                }
                if (!allIndices.isEmpty() && !bindings.containsKey(attrName + "_index")) {
                    bindings.put(attrName + "_index", allIndices);
                }
                log.debug("Predicate placeholder bound {} to {} colrefs, {} indices",
                        attrName, allColRefs.size(), allIndices.size());
            }
        }
    }

    /**
     * Recursively collect all RexInputRef indices and their ColumnRefs from a RexNode tree.
     * Uses a LinkedHashSet to deduplicate while preserving order.
     */
    private void collectAllRexInputRefs(RexNode node, List<ColumnRef> colRefs, List<Integer> indices) {
        Set<Integer> seen = new LinkedHashSet<>();
        collectAllRexInputRefsInternal(node, colRefs, indices, seen);
    }

    private void collectAllRexInputRefsInternal(RexNode node, List<ColumnRef> colRefs, List<Integer> indices, Set<Integer> seen) {
        if (node instanceof RexInputRef) {
            RexInputRef ref = (RexInputRef) node;
            int idx = ref.getIndex();
            if (seen.add(idx)) {
                indices.add(idx);
                if (queryOperator != null) {
                    colRefs.add(ColumnRefResolver.resolve(idx, queryOperator));
                }
            }
        } else if (node instanceof RexCall) {
            for (RexNode operand : ((RexCall) node).getOperands()) {
                collectAllRexInputRefsInternal(operand, colRefs, indices, seen);
            }
        } else if (node instanceof RexSubQuery) {
            for (RexNode operand : ((RexSubQuery) node).getOperands()) {
                collectAllRexInputRefsInternal(operand, colRefs, indices, seen);
            }
        }
    }
}
