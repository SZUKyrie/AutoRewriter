package org.autorewriter.rewriter.pipleline;

import groovy.util.logging.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.autorewriter.rewriter.pipleline.result.ProduceResult;

@Slf4j
public abstract class ProducePipeline {
    protected abstract ProduceStage lastStage();

    //protected abstract void Optimize();

    protected abstract ProduceResult runTheLogic(ProduceStage lastStage, ProduceContext context);

    public ProduceResult run (ProduceContext context) {
        ProduceStage lastStage = lastStage();
        ProduceResult produceResult = new ProduceResult();
        try {
            produceResult = runTheLogic(lastStage, context);
        } catch(Exception e) {
            produceResult.setSuccess(false);
            produceResult.setErrorMessage(ExceptionUtils.getStackTrace(e));
        }
        return produceResult;
    }
}
