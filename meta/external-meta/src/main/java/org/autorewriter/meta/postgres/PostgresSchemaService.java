package org.autorewriter.meta.postgres;

import org.apache.calcite.schema.Table;
import org.autorewriter.meta.schema.AbstractSchemaService;

import java.util.List;

public class PostgresSchemaService extends AbstractSchemaService {

    @Override
    public Table getTable(List<String> parents, String tableName) throws Exception {
        return null;
    }
}
