package org.autorewriter.rewriter.historical;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class HistoricalSqlRecord {
    String queryId;
    String sql;
}
