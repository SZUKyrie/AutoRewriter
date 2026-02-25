package org.autorewriter.rewriter.pipleline.result;

import groovy.util.logging.Slf4j;
import lombok.Getter;
import lombok.Setter;
import org.autorewriter.rewriter.optimize.OptimizeResult;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Setter
@Getter
public class ProduceResult {
    private boolean success;
    private String errorMessage;
    private List<OptimizeResult> optimizeResults = new ArrayList<>();
    /** Time spent building and registering all rules into the optimizer (ms) */
    private long ruleRegistrationTimeMs;
}
