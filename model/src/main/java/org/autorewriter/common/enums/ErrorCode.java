package org.autorewriter.common.enums;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum ErrorCode {
    SUCCESS(200, "success"),
    SQL_ANALYZE_ERROR(2000, "sql analyze error"),
    SQL_PARSE_ERROR(2001, "sql parse error"),
    RULE_ANALYZE_ERROR(2002, "rule analyze error");

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
