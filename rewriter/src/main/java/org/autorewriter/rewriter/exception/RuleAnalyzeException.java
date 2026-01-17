package org.autorewriter.rewriter.exception;

import org.autorewriter.common.enums.ErrorCode;
import org.autorewriter.common.exception.AutoRewriterException;

public class RuleAnalyzeException extends AutoRewriterException {
    public RuleAnalyzeException(String message) {
        super(ErrorCode.RULE_ANALYZE_ERROR, message);
    }

    public RuleAnalyzeException(String message, Throwable cause) {
        super(ErrorCode.RULE_ANALYZE_ERROR, message, cause);
    }

    public RuleAnalyzeException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public RuleAnalyzeException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
