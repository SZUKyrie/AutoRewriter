package org.autorewriter.rewriter.rule.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A Union-Find (disjoint set) data structure for tracking equivalence classes.
 *
 * <p>Ported from WeTune's {@code NaturalCongruence} / {@code BaseCongruence}.
 * Elements can be grouped into equivalence classes; the structure supports
 * efficient merging and querying of those classes.
 *
 * <p>Internally, each element maps to a shared {@link CongruentClass} set.
 * When two classes are merged, one absorbs the other and all map entries
 * are updated to point to the surviving class.
 *
 * @param <T> the element type (must have correct {@code equals}/{@code hashCode})
 */
public class NaturalCongruence<T> {

    /**
     * A named inner class for equivalence class sets. Extends {@link LinkedHashSet}
     * to preserve insertion order, which is useful for deterministic iteration.
     */
    private static final class CongruentClass<T> extends LinkedHashSet<T> {
        private static final long serialVersionUID = 1L;
    }

    private final Map<T, CongruentClass<T>> map;

    private NaturalCongruence() {
        this.map = new LinkedHashMap<>();
    }

    /**
     * Creates a new empty congruence.
     *
     * @param <T> the element type
     * @return a fresh, empty {@code NaturalCongruence}
     */
    public static <T> NaturalCongruence<T> create() {
        return new NaturalCongruence<>();
    }

    /**
     * Registers {@code x} in the congruence and returns its equivalence class.
     *
     * <p>If {@code x} is already registered, the existing class is returned.
     * Otherwise a new singleton equivalence class containing {@code x} is created.
     *
     * @param x the element to register
     * @return the (possibly new) equivalence class set containing {@code x}
     */
    public Set<T> mkEqClass(T x) {
        CongruentClass<T> cls = map.get(x);
        if (cls != null) {
            return cls;
        }
        CongruentClass<T> newClass = new CongruentClass<>();
        newClass.add(x);
        map.put(x, newClass);
        return newClass;
    }

    /**
     * Merges the equivalence classes of {@code x} and {@code y}.
     *
     * <p>If either element is not yet registered, it is registered first.
     * If both elements already belong to the same class, this is a no-op.
     * Otherwise one class absorbs the other: all elements of the smaller class
     * are added to the larger one and their map entries are updated.
     *
     * @param x the first element
     * @param y the second element
     */
    public void putCongruent(T x, T y) {
        mkEqClass(x);
        mkEqClass(y);
        CongruentClass<T> classX = map.get(x);
        CongruentClass<T> classY = map.get(y);
        if (classX == classY) {
            return; // already in the same equivalence class
        }
        // Merge smaller into larger for efficiency
        CongruentClass<T> larger;
        CongruentClass<T> smaller;
        if (classX.size() >= classY.size()) {
            larger = classX;
            smaller = classY;
        } else {
            larger = classY;
            smaller = classX;
        }
        larger.addAll(smaller);
        for (T element : smaller) {
            map.put(element, larger);
        }
    }

    /**
     * Returns {@code true} if {@code x} and {@code y} are in the same equivalence class.
     *
     * <p>Reflexivity: {@code isCongruent(a, a)} always returns {@code true},
     * even if {@code a} has never been registered.
     *
     * @param x the first element
     * @param y the second element
     * @return {@code true} if {@code x} and {@code y} are congruent
     */
    public boolean isCongruent(T x, T y) {
        if (x.equals(y)) {
            return true;
        }
        CongruentClass<T> classX = map.get(x);
        CongruentClass<T> classY = map.get(y);
        return classX != null && classX == classY;
    }

    /**
     * Returns the equivalence class of {@code x}.
     *
     * <p>If {@code x} has been registered (via {@link #mkEqClass} or
     * {@link #putCongruent}), the live mutable set is returned.
     * If {@code x} has never been registered, an unmodifiable singleton
     * set is returned instead.
     *
     * @param x the element to look up
     * @return the set of all elements equivalent to {@code x}
     */
    public Set<T> eqClassOf(T x) {
        CongruentClass<T> cls = map.get(x);
        if (cls != null) {
            return cls;
        }
        return Collections.singleton(x);
    }
}
