package org.autorewriter.rewriter.pipleline;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.autorewriter.common.enums.ComputeEngine;
import org.autorewriter.rewriter.analyze.RuleAnalysisContext;
import org.autorewriter.rewriter.historical.HistoricalSqlRecord;

import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
public class ProduceContext {
    private final Map<String, HistoricalSqlRecord> queryId2HistoricalSqlRecord;
    private final List<RuleAnalysisContext> ruleAnalysisContexts;
    private final ComputeEngine computeEngine;

    public ProduceContext(Map<String, HistoricalSqlRecord> queryId2HistoricalSqlRecord, List<RuleAnalysisContext> ruleAnalysisContexts, ComputeEngine computeEngine) {
        this.queryId2HistoricalSqlRecord = queryId2HistoricalSqlRecord;
        this.ruleAnalysisContexts = ruleAnalysisContexts;
        this.computeEngine = computeEngine;
    }
}
