package org.autorewriter.common.exception;

import lombok.Getter;
import org.autorewriter.common.enums.ErrorCode;

@Getter
public class AutoRewriterException extends RuntimeException{
    private final ErrorCode code;

    public AutoRewriterException(ErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public AutoRewriterException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public AutoRewriterException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    @Override
    public String toString() {
        return super.toString() + "\n" + getCode();
    }
}
