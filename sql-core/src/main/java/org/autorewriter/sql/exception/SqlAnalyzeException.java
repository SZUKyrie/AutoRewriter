package org.autorewriter.sql.exception;


import org.autorewriter.common.enums.ErrorCode;
import org.autorewriter.common.exception.AutoRewriterException;

public class SqlAnalyzeException extends AutoRewriterException {
    public SqlAnalyzeException(String message) {
        super(ErrorCode.SQL_ANALYZE_ERROR, message);
    }

    public SqlAnalyzeException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
