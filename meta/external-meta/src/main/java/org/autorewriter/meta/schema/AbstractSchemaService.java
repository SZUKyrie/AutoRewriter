package org.autorewriter.meta.schema;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.Function;
import org.apache.calcite.schema.impl.ScalarFunctionImpl;
import org.apache.calcite.schema.impl.TableFunctionImpl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractSchemaService implements SchemaService{
    private final Multimap<String, Function> functionMultimap = HashMultimap.create();

    private final Map<String, RelProtoDataType> typeMap = new HashMap<>();

    @Override
    public ProxySchema getSubSchema(List<String> parents, String subSchemaName) throws Exception {
        if (!parents.isEmpty()) {
            return null;
        }
        ProxySchema proxySchema = new ProxySchema(ImmutableList.of(subSchemaName), this);
        return proxySchema;
    }

    @Override
    public Multimap<String, Function> getFunctionMultimap() {
        return functionMultimap;
    }

    @Override
    public void registerFunction(String functionName, Function function) {
        functionMultimap.put(functionName, function);
    }

    @Override
    public Map<String, RelProtoDataType> getTypeMap() {
        return typeMap;
    }

    @Override
    public void registerType(String typeName, RelProtoDataType type) {
        typeMap.put(typeName, type);
    }

    protected void registerScalarFunction(Class<?> clazz) {
        registerFunctions(clazz, method -> ScalarFunctionImpl.create(method));
    }

    protected void registerTableFunction(Class<?> clazz) {
        registerFunctions(clazz, method -> TableFunctionImpl.create(method));
    }

    private void registerFunctions(Class<?> clazz, FunctionFactory functionFactory) {
        // TODO
    }

    @FunctionalInterface
    private interface FunctionFactory {
        Function create(Method method);
    }
}
