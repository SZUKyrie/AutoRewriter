package org.autorewriter.meta.schema;

import java.util.Collection;

import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.Function;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaVersion;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.TableMacro;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMap.Builder;
import com.google.common.collect.ImmutableSortedSet;

/**
 * Calcite schema in AutoRewriter,
 * get sub schema and table method will be delivered to proxy schema,
 * other method will be empty or not supported.
 *
 * 完全参考 adaptiveengine 的 KwaiCalciteSchema 实现
 *
 * @author wangyanjing <wangyanjing@kuaishou.com>
 * Created on 2024-07-29
 */
public class KwaiCalciteSchema extends CalciteSchema {
    public KwaiCalciteSchema(@Nullable CalciteSchema parent, ProxySchema schema, String name) {
        super(parent, schema, name, null, null, null, null,
                null, null, null, null);
    }

    @Override
    protected @Nullable CalciteSchema getImplicitSubSchema(String schemaName, boolean caseSensitive) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(schemaName), "schemaName can't be empty");
        // use lower case schema name to get table when case-insensitive
        schemaName = caseSensitive ? schemaName : schemaName.toLowerCase();
        ProxySchema currentSchema = (ProxySchema) this.schema;
        ProxySchema subSchema = currentSchema.getSubSchema(schemaName);
        if (subSchema == null) {
            return null;
        }
        return new KwaiCalciteSchema(this, subSchema, schemaName);
    }

    @Override
    protected TableEntry getImplicitTable(String tableName, boolean caseSensitive) {
        tableName = caseSensitive ? tableName : tableName.toLowerCase();
        ProxySchema currentSchema = (ProxySchema) this.schema;
        final Table table = currentSchema.getTable(tableName);
        if (table == null) {
            return null;
        }
        return tableEntry(tableName, table);
    }

    @Override
    protected TypeEntry getImplicitType(String name, boolean caseSensitive) {
        name = caseSensitive ? name : name.toLowerCase();
        final RelProtoDataType type = schema.getType(name);
        if (type == null) {
            return null;
        }
        return typeEntry(name, type);
    }

    @Override
    protected TableEntry getImplicitTableBasedOnNullaryFunction(String tableName, boolean caseSensitive) {
        Collection<Function> functions = schema.getFunctions(tableName);
        if (functions == null) {
            return null;
        }
        for (Function function : functions) {
            if (function instanceof TableMacro && function.getParameters().isEmpty()) {
                final Table table = ((TableMacro) function).apply(ImmutableList.of());
                return tableEntry(tableName, table);
            }
        }
        return null;
    }

    @Override
    protected void addImplicitSubSchemaToBuilder(Builder<String, CalciteSchema> builder) {
        // ignore, calcite schema of kwai shou doesn't check if schema is correct or not
    }

    @Override
    protected void addImplicitTableToBuilder(ImmutableSortedSet.Builder<String> builder) {

    }

    @Override
    protected void addImplicitFunctionsToBuilder(ImmutableList.Builder<Function> builder, String name,
                                                  boolean caseSensitive) {
        Collection<Function> functions = schema.getFunctions(name);
        if (functions != null) {
            builder.addAll(functions);
        }
    }

    @Override
    protected void addImplicitFuncNamesToBuilder(ImmutableSortedSet.Builder<String> builder) {
        Collection<String> functionNames = schema.getFunctionNames();
        if (functionNames != null) {
            builder.addAll(functionNames);
        }
    }

    @Override
    protected void addImplicitTypeNamesToBuilder(ImmutableSortedSet.Builder<String> builder) {
        builder.addAll(schema.getTypeNames());
    }

    @Override
    protected void addImplicitTablesBasedOnNullaryFunctionsToBuilder(Builder<String, Table> builder) {
        ImmutableSortedMap<String, Table> explicitTables = builder.build();
        for (String s : schema.getFunctionNames()) {
            // explicit table wins.
            if (explicitTables.containsKey(s)) {
                continue;
            }
            for (Function function : schema.getFunctions(s)) {
                if (function instanceof TableMacro
                        && function.getParameters().isEmpty()) {
                    final Table table = ((TableMacro) function).apply(ImmutableList.of());
                    builder.put(s, table);
                }
            }
        }
    }

    @Override
    protected CalciteSchema snapshot(@Nullable CalciteSchema parent, SchemaVersion version) {
        return this;
    }

    @Override
    protected boolean isCacheEnabled() {
        return false;
    }

    @Override
    public void setCache(boolean cache) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CalciteSchema add(String name, Schema schema) {
        throw new UnsupportedOperationException();
    }
}

