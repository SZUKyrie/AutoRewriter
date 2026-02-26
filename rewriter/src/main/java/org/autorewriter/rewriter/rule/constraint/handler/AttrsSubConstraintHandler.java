package org.autorewriter.rewriter.rule.constraint.handler;

import lombok.extern.slf4j.Slf4j;
import org.autorewriter.rewriter.rule.constraint.ConstraintHandler;

import java.util.Map;

@Slf4j
public class AttrsSubConstraintHandler implements ConstraintHandler {

    @Override
    public String getType() {
        return "ATTRS_SUB";
    }

    @Override
    public boolean evaluate(String[] params, Map<String, Object> bindings) {
        boolean result = bindings.containsKey(params[0]) || bindings.containsKey(params[0] + "_index");
        log.debug("AttrsSub({}): bound={}", params[0], result);
        return result;
    }
}

