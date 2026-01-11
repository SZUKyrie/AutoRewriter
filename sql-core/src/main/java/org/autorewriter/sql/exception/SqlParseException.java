package org.autorewriter.sql.exception;

import lombok.Getter;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.shardingsphere.sql.parser.engine.exception.SQLParsingException;
import org.autorewriter.common.enums.ErrorCode;
import org.autorewriter.common.exception.AutoRewriterException;

@Getter
public class SqlParseException extends AutoRewriterException {

    private final int line;

    public SqlParseException(String message) {
        super(ErrorCode.SQL_PARSE_ERROR, message);
        this.line = -1;
    }

    public SqlParseException(Throwable cause) {
        super(ErrorCode.SQL_PARSE_ERROR, cause.getMessage(), cause);
        if (cause instanceof SQLParsingException) {
            SQLParsingException sqlParsingException = (SQLParsingException) cause;
            this.line = sqlParsingException.getLine();
        } else if (cause instanceof org.apache.calcite.sql.parser.SqlParseException) {
            SqlParserPos pos = ((org.apache.calcite.sql.parser.SqlParseException) cause).getPos();
            this.line = pos.getLineNum();
        } else {
            this.line = -1;
        }
    }
}
