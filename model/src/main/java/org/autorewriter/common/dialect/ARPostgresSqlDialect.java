package org.autorewriter.common.dialect;

import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;

public class ARPostgresSqlDialect extends PostgresqlSqlDialect {

    public ARPostgresSqlDialect(Context context) {super(context);}

    @Override
    public boolean supportsSQL99Between() {
        return false;
    }


}
