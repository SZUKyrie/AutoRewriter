package org.autorewriter.meta.schema;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.autorewriter.common.utils.TableNameUtils.getQualifiedNameString;
import static java.util.Objects.requireNonNull;

/**
 * Schema is level-structured, like catalog -> database, database is sub schema of catalog schema
 * This schema will be proxied to {@link SchemaService} to find all tables under parents.
 * Any intermediate sub schema will not be checked for whether it is correct, such check will happen when get table.
 *
 * @author wangyanjing <wangyanjing@kuaishou.com>
 * Created on 2024-07-03
 */
public class ProxySchema implements Schema {

    /*
     * Parents is parent schemas, values are [], [catalog], [catalog, database] style.
     */
    private List<String> parents;
    private SchemaService schemaService;

    public ProxySchema(List<String> parents, SchemaService schemaService) {
        Preconditions.checkNotNull(parents, "parents mustn't be null");
        this.parents = parents;
        this.schemaService = schemaService;
    }

    @Override
    public @Nullable Table getTable(String name) {
        try {
            return schemaService.getTable(parents, name);
        } catch (Exception e) {
            throw new RuntimeException("raise exception when get table " + getQualifiedNameString(parents, name), e);
        }
    }

    @Override
    public Set<String> getTableNames() {
        return ImmutableSet.of();
    }

    @Override
    public @Nullable RelProtoDataType getType(String name) {
        Map<String, RelProtoDataType> typeMap = schemaService.getTypeMap();
        return typeMap.get(name);
    }

    @Override
    public Set<String> getTypeNames() {
        return schemaService.getTypeMap().keySet();
    }

    @Override
    public Collection<Function> getFunctions(String name) {
        Multimap<String, Function> functionMultimap = schemaService.getFunctionMultimap();
        return functionMultimap.get(name);
    }

    @Override
    public Set<String> getFunctionNames() {
        return schemaService.getFunctionMultimap().keySet();
    }

    @Override
    public @Nullable ProxySchema getSubSchema(String name) {
        try {
            return schemaService.getSubSchema(parents, name);
        } catch (Exception e) {
            throw new RuntimeException("raise exception when get sub schema " + getQualifiedNameString(parents, name), e);
        }
    }

    @Override
    public Set<String> getSubSchemaNames() {
        return ImmutableSet.of();
    }

    @Override
    public Expression getExpression(@Nullable SchemaPlus parentSchema, String name) {
        requireNonNull(parentSchema, "parentSchema");
        return Schemas.subSchemaExpression(parentSchema, name, getClass());
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public ProxySchema snapshot(SchemaVersion version) {
        return this;
    }
}
