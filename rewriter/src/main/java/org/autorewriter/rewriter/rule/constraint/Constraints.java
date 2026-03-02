package org.autorewriter.rewriter.rule.constraint;

import org.apache.shardingsphere.sql.parser.api.ASTNode;
import org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment;
import org.autorewriter.rewriter.rule.model.NaturalCongruence;
import org.autorewriter.rewriter.rule.symbol.Symbol;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A structured, immutable collection of {@link Constraint}s extracted from a WeTune-style
 * rewrite rule.
 *
 * <p>On construction, the class separates cross-side equality constraints (captured as an
 * <em>instantiation map</em>) from same-side equality constraints (captured in a
 * {@link NaturalCongruence} union-find). The remaining constraints are sorted by
 * {@link ConstraintKind} ordinal and indexed for fast {@link #ofKind} access.
 *
 * <p>This class extends {@link AbstractList} so that it can be iterated and indexed like
 * a regular {@code List<Constraint>}.
 */
public class Constraints extends AbstractList<Constraint> {

    /** Sorted array of non-cross-side constraints. */
    private final Constraint[] items;

    /** Segment base indices for fast ofKind() access. One entry per ConstraintKind ordinal + 1. */
    private final int[] segBases;

    /** Union-Find equivalence classes for source-side equality symbols. */
    private final NaturalCongruence<Symbol> eqSymbols;

    /** Mapping from target symbol → source symbol for cross-side equality constraints. */
    private final Map<Symbol, Symbol> instantiation;

    private Constraints(Constraint[] items,
                        int[] segBases,
                        NaturalCongruence<Symbol> eqSymbols,
                        Map<Symbol, Symbol> instantiation) {
        this.items = items;
        this.segBases = segBases;
        this.eqSymbols = eqSymbols;
        this.instantiation = instantiation;
    }

    // ── Factory: from ShardingSphere AST nodes ──────────────────────────

    /**
     * Builds a {@code Constraints} instance from parsed ShardingSphere
     * {@link ConstraintSegment} AST nodes.
     *
     * @param constraintSegments the raw AST nodes (only {@link ConstraintSegment} instances
     *                           are processed; others are silently skipped)
     * @param sourceSymbols      map of placeholder name → {@link Symbol} for the source side
     * @param targetSymbols      map of placeholder name → {@link Symbol} for the target side
     * @return a fully constructed {@code Constraints}
     */
    public static Constraints build(List<ASTNode> constraintSegments,
                                    Map<String, Symbol> sourceSymbols,
                                    Map<String, Symbol> targetSymbols) {
        List<Constraint> raw = new ArrayList<>();
        for (ASTNode node : constraintSegments) {
            if (!(node instanceof ConstraintSegment)) {
                continue;
            }
            ConstraintSegment cs = (ConstraintSegment) node;
            ConstraintKind kind = ConstraintKind.fromShardingSphere(cs.getType().name());
            String[] params = cs.getParams();
            Symbol[] syms = new Symbol[params.length];
            for (int i = 0; i < params.length; i++) {
                Symbol sym = sourceSymbols.get(params[i]);
                if (sym == null) {
                    sym = targetSymbols.get(params[i]);
                }
                if (sym == null) {
                    throw new IllegalArgumentException(
                            "Symbol not found for param: " + params[i]);
                }
                syms[i] = sym;
            }
            raw.add(Constraint.of(kind, syms));
        }

        Set<String> srcKeys = sourceSymbols.keySet();
        return buildInternal(raw, srcKeys, sourceSymbols, targetSymbols);
    }

    // ── Factory: from pre-built Constraint objects (for testing) ────────

    /**
     * Builds a {@code Constraints} instance from pre-built {@link Constraint} objects.
     *
     * <p>This factory is intended for unit tests where constructing real
     * {@link ConstraintSegment} instances is inconvenient.
     *
     * @param constraints   the list of constraints
     * @param sourceSymbols set of symbols that belong to the source side
     * @param targetSymbols set of symbols that belong to the target side
     * @return a fully constructed {@code Constraints}
     */
    public static Constraints build(List<Constraint> constraints,
                                    Set<Symbol> sourceSymbols,
                                    Set<Symbol> targetSymbols) {
        // Convert sets to name→Symbol maps for internal use
        Map<String, Symbol> srcMap = new HashMap<>();
        for (Symbol s : sourceSymbols) {
            srcMap.put(s.name(), s);
        }
        Map<String, Symbol> tgtMap = new HashMap<>();
        for (Symbol s : targetSymbols) {
            tgtMap.put(s.name(), s);
        }
        return buildInternal(new ArrayList<>(constraints), srcMap.keySet(), srcMap, tgtMap);
    }

    // ── Shared construction logic ───────────────────────────────────────

    private static Constraints buildInternal(List<Constraint> raw,
                                             Set<String> srcKeys,
                                             Map<String, Symbol> sourceSymbols,
                                             Map<String, Symbol> targetSymbols) {
        NaturalCongruence<Symbol> eqSymbols = NaturalCongruence.create();
        Map<Symbol, Symbol> instantiation = new HashMap<>();
        List<Constraint> remaining = new ArrayList<>();

        for (Constraint c : raw) {
            if (c.kind().isEq()) {
                Symbol[] syms = c.symbols();
                Symbol s0 = syms[0];
                Symbol s1 = syms[1];
                boolean s0Source = srcKeys.contains(s0.name());
                boolean s1Source = srcKeys.contains(s1.name());

                if (s0Source && s1Source) {
                    // Both source-side → add to union-find
                    eqSymbols.putCongruent(s0, s1);
                    remaining.add(c);
                } else if (s0Source && !s1Source) {
                    // s0 is source, s1 is target → instantiation
                    instantiation.put(s1, s0);
                } else if (!s0Source && s1Source) {
                    // s0 is target, s1 is source → instantiation
                    instantiation.put(s0, s1);
                } else {
                    // Both target-side — unusual, but keep it
                    remaining.add(c);
                }
            } else {
                remaining.add(c);
            }
        }

        // Sort remaining by kind ordinal for segment-based access
        remaining.sort((a, b) -> Integer.compare(a.kind().ordinal(), b.kind().ordinal()));

        // Build segment base array
        int kindCount = ConstraintKind.values().length;
        int[] segBases = new int[kindCount + 1];
        int idx = 0;
        for (int k = 0; k < kindCount; k++) {
            segBases[k] = idx;
            while (idx < remaining.size() && remaining.get(idx).kind().ordinal() == k) {
                idx++;
            }
        }
        segBases[kindCount] = remaining.size();

        Constraint[] items = remaining.toArray(new Constraint[0]);
        return new Constraints(items, segBases, eqSymbols, Collections.unmodifiableMap(instantiation));
    }

    // ── AbstractList implementation ─────────────────────────────────────

    @Override
    public Constraint get(int index) {
        return items[index];
    }

    @Override
    public int size() {
        return items.length;
    }

    // ── Query API ───────────────────────────────────────────────────────

    /**
     * Returns the union-find structure for source-side equality symbols.
     *
     * @return the {@link NaturalCongruence} for source-side equivalences
     */
    public NaturalCongruence<Symbol> eqSymbols() {
        return eqSymbols;
    }

    /**
     * Returns the source-side symbol that a target symbol is instantiated from,
     * or {@code null} if no cross-side equality constraint maps the given target symbol.
     *
     * @param targetSym the target-side symbol to look up
     * @return the corresponding source-side symbol, or {@code null}
     */
    public Symbol instantiationOf(Symbol targetSym) {
        return instantiation.get(targetSym);
    }

    /**
     * Finds the table symbol that an attrs symbol is a subset of, by searching
     * {@link ConstraintKind#ATTRS_SUB} constraints.
     *
     * <p>Returns the {@code symbols()[1]} (the table symbol) of the first
     * {@code ATTRS_SUB} constraint whose {@code symbols()[0]} equals {@code attrsSym}.
     *
     * @param attrsSym the attrs symbol to look up
     * @return the corresponding table symbol, or {@code null} if not found
     */
    public Symbol sourceOf(Symbol attrsSym) {
        for (Constraint c : ofKind(ConstraintKind.ATTRS_SUB)) {
            Symbol[] syms = c.symbols();
            if (syms[0].equals(attrsSym)) {
                return syms[1];
            }
        }
        return null;
    }

    /**
     * Returns a view of all constraints of the given kind.
     *
     * @param kind the constraint kind to filter by
     * @return an unmodifiable list of constraints of that kind (may be empty, never null)
     */
    public List<Constraint> ofKind(ConstraintKind kind) {
        int ord = kind.ordinal();
        int from = segBases[ord];
        int to = segBases[ord + 1];
        if (from == to) {
            return Collections.emptyList();
        }
        // Return a subList view backed by this AbstractList
        return this.subList(from, to);
    }

    /**
     * Returns {@code true} if symbols {@code s0} and {@code s1} are in the same
     * source-side equivalence class.
     *
     * @param s0 the first symbol
     * @param s1 the second symbol
     * @return {@code true} if congruent
     */
    public boolean isEq(Symbol s0, Symbol s1) {
        return eqSymbols.isCongruent(s0, s1);
    }

    /**
     * Returns the equivalence class of the given symbol under source-side equality.
     *
     * @param symbol the symbol to look up
     * @return the set of all symbols equivalent to {@code symbol}
     */
    public Set<Symbol> eqClassOf(Symbol symbol) {
        return eqSymbols.eqClassOf(symbol);
    }
}
