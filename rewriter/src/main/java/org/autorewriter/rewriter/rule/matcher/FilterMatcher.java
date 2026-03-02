package org.autorewriter.rewriter.rule.matcher;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexSubQuery;
import org.autorewriter.rewriter.rule.RelNodeMatcher;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.autorewriter.rewriter.rule.util.ColumnRefResolver;
import org.autorewriter.rewriter.rule.util.RexNodeMatcher;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

@Slf4j
@AllArgsConstructor
public class FilterMatcher implements RelNodeMatcher<LogicalFilter> {

    private final BiFunction<org.apache.calcite.rel.RelNode, org.apache.calcite.rel.RelNode, Boolean> recursiveMatchFunc;
    private final RexNodeMatcher rexNodeMatcher;

    @Override
    public boolean match(LogicalFilter template, LogicalFilter query, Map<String, Object> bindings) {
        if (!recursiveMatchFunc.apply(template.getInput(), query.getInput())) {
            return false;
        }

        rexNodeMatcher.setQueryOperator(query.getInput());
        boolean result = rexNodeMatcher.match(template.getCondition(), query.getCondition(), bindings);
        if (result) {
            bindFilterAttributes(template, query, bindings);
        }

        return result;
    }

    private void bindFilterAttributes(LogicalFilter template, LogicalFilter query, Map<String, Object> bindings) {
        List<String> templateFields = template.getRowType().getFieldNames();
        List<String> attrPlaceholders = new ArrayList<>();
        for (String f : templateFields) {
            if (f.matches("a\\d+")) {
                attrPlaceholders.add(f);
            }
        }
        if (attrPlaceholders.isEmpty()) {
            return;
        }

        List<ColumnRef> allColRefs = new ArrayList<>();
        List<Integer> allIndices = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        collectAllRexInputRefs(query.getCondition(), query, allColRefs, allIndices, seen);

        for (String attr : attrPlaceholders) {
            if (!bindings.containsKey(attr + "_colref")) {
                if (!allColRefs.isEmpty()) {
                    bindings.put(attr + "_colref", allColRefs);
                }
                if (!allIndices.isEmpty()) {
                    bindings.put(attr + "_index", allIndices);
                }
                log.debug("Filter bound {} to {} colrefs from condition", attr, allColRefs.size());
            }
        }
    }

    private void collectAllRexInputRefs(RexNode node, LogicalFilter queryFilter,
                                         List<ColumnRef> colRefs, List<Integer> indices, Set<Integer> seen) {
        if (node instanceof RexInputRef) {
            RexInputRef ref = (RexInputRef) node;
            int idx = ref.getIndex();
            if (seen.add(idx)) {
                indices.add(idx);
                colRefs.add(ColumnRefResolver.resolve(idx, queryFilter.getInput()));
            }
        } else if (node instanceof RexCall) {
            for (RexNode operand : ((RexCall) node).getOperands()) {
                collectAllRexInputRefs(operand, queryFilter, colRefs, indices, seen);
            }
        } else if (node instanceof RexSubQuery) {
            for (RexNode operand : ((RexSubQuery) node).getOperands()) {
                collectAllRexInputRefs(operand, queryFilter, colRefs, indices, seen);
            }
        }
    }
}
