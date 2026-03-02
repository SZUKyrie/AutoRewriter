package org.autorewriter.rewriter.rule.constraint;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.util.ImmutableBitSet;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.autorewriter.rewriter.rule.util.ColumnRefResolver;

import java.util.*;

public final class BindingResolver {

    private BindingResolver() {}

    public static List<ColumnRef> resolveColRefs(String attrParam, Map<String, Object> bindings) {
        Object colRefObj = bindings.get(attrParam + "_colref");
        if (colRefObj instanceof ColumnRef) {
            return Collections.singletonList((ColumnRef) colRefObj);
        }
        if (colRefObj instanceof List) {
            List<?> list = (List<?>) colRefObj;
            List<ColumnRef> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof ColumnRef) {
                    result.add((ColumnRef) item);
                }
            }
            return result.isEmpty() ? null : result;
        }
        return null;
    }

    public static List<ColumnRef> resolveColRefs(String attrParam, Map<String, Object> bindings, RelNode rel) {
        List<ColumnRef> fromColRef = resolveColRefs(attrParam, bindings);
        if (fromColRef != null) {
            return fromColRef;
        }

        Object idxObj = bindings.get(attrParam + "_index");
        if (idxObj instanceof Integer) {
            int idx = (Integer) idxObj;
            if (idx < rel.getRowType().getFieldCount()) {
                return Collections.singletonList(ColumnRefResolver.resolve(idx, rel));
            }
        }
        if (idxObj instanceof List) {
            List<Integer> indices = (List<Integer>) idxObj;
            List<ColumnRef> result = new ArrayList<>();
            for (int idx : indices) {
                if (idx < rel.getRowType().getFieldCount()) {
                    result.add(ColumnRefResolver.resolve(idx, rel));
                }
            }
            return result.isEmpty() ? null : result;
        }

        return null;
    }

    public static Set<ColumnRef> resolveColRefSet(String attrParam, Map<String, Object> bindings, RelNode rel) {
        List<ColumnRef> list = resolveColRefs(attrParam, bindings, rel);
        return list != null ? new LinkedHashSet<>(list) : null;
    }

    @SuppressWarnings("unchecked")
    public static ImmutableBitSet resolveColBits(String attrParam, Map<String, Object> bindings, RelNode rel) {
        Object colRefObj = bindings.get(attrParam + "_colref");
        if (colRefObj instanceof ColumnRef) {
            int idx = ColumnRefResolver.resolveIndex((ColumnRef) colRefObj, rel);
            return idx >= 0 ? ImmutableBitSet.of(idx) : null;
        }
        if (colRefObj instanceof List) {
            List<ColumnRef> colRefs = (List<ColumnRef>) colRefObj;
            List<Integer> indices = new ArrayList<>();
            for (ColumnRef cr : colRefs) {
                int idx = ColumnRefResolver.resolveIndex(cr, rel);
                if (idx >= 0) {
                    indices.add(idx);
                }
            }
            return indices.isEmpty() ? null : ImmutableBitSet.of(indices);
        }

        Object idxObj = bindings.get(attrParam + "_index");
        if (idxObj instanceof Integer) {
            ColumnRef cr = ColumnRefResolver.resolve((Integer) idxObj, rel);
            int resolvedIdx = ColumnRefResolver.resolveIndex(cr, rel);
            return resolvedIdx >= 0 ? ImmutableBitSet.of(resolvedIdx) : null;
        }
        if (idxObj instanceof List) {
            List<Integer> rawIndices = (List<Integer>) idxObj;
            List<Integer> resolvedIndices = new ArrayList<>();
            for (int rawIdx : rawIndices) {
                ColumnRef cr = ColumnRefResolver.resolve(rawIdx, rel);
                int resolvedIdx = ColumnRefResolver.resolveIndex(cr, rel);
                if (resolvedIdx >= 0) {
                    resolvedIndices.add(resolvedIdx);
                }
            }
            return resolvedIndices.isEmpty() ? null : ImmutableBitSet.of(resolvedIndices);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<Integer> resolveIndices(String attrParam, Map<String, Object> bindings, RelNode rel) {
        Object colRefObj = bindings.get(attrParam + "_colref");
        if (colRefObj instanceof ColumnRef) {
            int idx = ColumnRefResolver.resolveIndex((ColumnRef) colRefObj, rel);
            return idx >= 0 ? Collections.singletonList(idx) : null;
        }
        if (colRefObj instanceof List) {
            List<ColumnRef> colRefs = (List<ColumnRef>) colRefObj;
            List<Integer> indices = new ArrayList<>();
            for (ColumnRef cr : colRefs) {
                int idx = ColumnRefResolver.resolveIndex(cr, rel);
                if (idx >= 0) {
                    indices.add(idx);
                }
            }
            return indices.isEmpty() ? null : indices;
        }

        Object idxObj = bindings.get(attrParam + "_index");
        if (idxObj instanceof Integer) {
            ColumnRef cr = ColumnRefResolver.resolve((Integer) idxObj, rel);
            int resolvedIdx = ColumnRefResolver.resolveIndex(cr, rel);
            return resolvedIdx >= 0 ? Collections.singletonList(resolvedIdx) : null;
        }
        if (idxObj instanceof List) {
            List<Integer> rawIndices = (List<Integer>) idxObj;
            List<Integer> resolvedIndices = new ArrayList<>();
            for (int rawIdx : rawIndices) {
                ColumnRef cr = ColumnRefResolver.resolve(rawIdx, rel);
                int resolvedIdx = ColumnRefResolver.resolveIndex(cr, rel);
                if (resolvedIdx >= 0) {
                    resolvedIndices.add(resolvedIdx);
                }
            }
            return resolvedIndices.isEmpty() ? null : resolvedIndices;
        }
        return null;
    }
}

