package org.autorewriter.rewriter.rule.constraint;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.shardingsphere.sql.parser.api.ASTNode;
import org.autorewriter.rewriter.rule.constraint.handler.*;

import java.util.*;

@Slf4j
public class ConstraintEvaluator {

    private final Map<String, ConstraintHandler> handlers = new HashMap<>();

    public ConstraintEvaluator() {
        registerHandler(new UniqueConstraintHandler());
        registerHandler(new NotNullConstraintHandler());
        registerHandler(new PredicateEqConstraintHandler());
        registerHandler(new AttrsEqConstraintHandler());
        registerHandler(new AttrsSubConstraintHandler());
        registerHandler(new TableEqConstraintHandler());
        registerHandler(new ReferenceConstraintHandler());
    }

    private void registerHandler(ConstraintHandler handler) {
        handlers.put(handler.getType(), handler);
    }

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

                ConstraintHandler handler = handlers.get(constraintType);
                if (handler == null) {
                    log.error("Unknown constraint type: {}", constraintType);
                    continue;
                }

                if (!handler.evaluate(params, bindings)) {
                    log.info("Constraint {} failed with params: {}", constraintType, Arrays.toString(params));
                    return false;
                }
            }
        }

        return true;
    }

    public Map<String, Object> applyRewriteConstraints(List<ASTNode> constraints, Map<String, Object> sourceBindings) {
        Map<String, Object> targetBindings = new HashMap<>(sourceBindings);

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
                        value instanceof RelNode ? ((RelNode) value).getClass().getSimpleName() : value);
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
