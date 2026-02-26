package org.autorewriter.rewriter.rule.constraint.handler;

import lombok.extern.slf4j.Slf4j;
import org.autorewriter.rewriter.rule.constraint.ConstraintHandler;

import java.util.Map;

/**
 * Reference(t0, a0, t1, a1): verifies t0 == t1 (same table) and a0 == a1 (same column).
 * Delegates to TableEqConstraintHandler and AttrsEqConstraintHandler.
 */
@Slf4j
public class ReferenceConstraintHandler implements ConstraintHandler {

    private final TableEqConstraintHandler tableEq = new TableEqConstraintHandler();
    private final AttrsEqConstraintHandler attrsEq = new AttrsEqConstraintHandler();

    @Override
    public String getType() {
        return "REFERENCE";
    }

    @Override
    public boolean evaluate(String[] params, Map<String, Object> bindings) {
        if (params.length < 4) {
            log.debug("Reference: insufficient params (need 4, got {})", params.length);
            return true;
        }

        boolean tableMatch = tableEq.evaluate(new String[]{params[0], params[2]}, bindings);
        if (!tableMatch) {
            log.debug("Reference({}, {}, {}, {}): table mismatch", params[0], params[1], params[2], params[3]);
            return false;
        }

        boolean attrMatch = attrsEq.evaluate(new String[]{params[1], params[3]}, bindings);
        if (!attrMatch) {
            log.debug("Reference({}, {}, {}, {}): attr mismatch", params[0], params[1], params[2], params[3]);
            return false;
        }

        log.debug("Reference({}, {}, {}, {}): matched", params[0], params[1], params[2], params[3]);
        return true;
    }
}

