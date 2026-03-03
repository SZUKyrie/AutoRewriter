package org.autorewriter.rewriter.rule.constraint;

import org.apache.shardingsphere.sql.parser.api.ASTNode;
import org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment;
import org.autorewriter.rewriter.rule.model.NaturalCongruence;
import org.autorewriter.rewriter.rule.symbol.Symbol;
import org.autorewriter.rewriter.rule.symbol.SymbolKind;

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
     * <p>Accepts match constraints and rewrite constraints separately so that
     * symbols not found in the extracted symbol maps (e.g., schema symbols that
     * the template parser doesn't embed in RelNode row types) can be correctly
     * classified as source-side or target-side:
     * <ul>
     *   <li>Match constraints: all symbols are source-side</li>
     *   <li>Rewrite constraints: for equality constraints, second param is source,
     *       first param is target; for other constraint types, all symbols are target-side</li>
     * </ul>
     *
     * @param matchConstraints   same-side (source) constraints
     * @param rewriteConstraints cross-side (target→source) constraints
     * @param sourceSymbols      map of placeholder name → {@link Symbol} for the source side
     * @param targetSymbols      map of placeholder name → {@link Symbol} for the target side
     * @return a fully constructed {@code Constraints}
     */
    public static Constraints build(List<ASTNode> matchConstraints,
                                    List<ASTNode> rewriteConstraints,
                                    Map<String, Symbol> sourceSymbols,
                                    Map<String, Symbol> targetSymbols) {
        // Create mutable copies so we can add auto-created symbols
        Map<String, Symbol> srcMap = new HashMap<>(sourceSymbols);
        Map<String, Symbol> tgtMap = new HashMap<>(targetSymbols);

        List<Constraint> raw = new ArrayList<>();

        // Process match constraints — unknown symbols are source-side
        for (ASTNode node : matchConstraints) {
            if (!(node instanceof ConstraintSegment)) continue;
            raw.add(parseConstraint((ConstraintSegment) node, srcMap, tgtMap, true));
        }

        // Process rewrite constraints — for eq constraints: param[0]=target, param[1]=source
        for (ASTNode node : rewriteConstraints) {
            if (!(node instanceof ConstraintSegment)) continue;
            raw.add(parseConstraint((ConstraintSegment) node, srcMap, tgtMap, false));
        }

        Set<String> srcKeys = srcMap.keySet();
        return buildInternal(raw, srcKeys, srcMap, tgtMap);
    }

    /**
     * Convenience overload that combines match and rewrite constraints into a single list.
     * Unknown symbols are auto-created if they match the symbol naming pattern.
     * Side classification uses a heuristic: if a peer of the same kind prefix exists
     * in one side, the unknown symbol goes to the other side.
     */
    public static Constraints build(List<ASTNode> constraintSegments,
                                    Map<String, Symbol> sourceSymbols,
                                    Map<String, Symbol> targetSymbols) {
        // Create mutable copies so we can add auto-created symbols
        Map<String, Symbol> srcMap = new HashMap<>(sourceSymbols);
        Map<String, Symbol> tgtMap = new HashMap<>(targetSymbols);

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
                syms[i] = resolveOrCreateSymbol(params[i], srcMap, tgtMap);
            }
            raw.add(Constraint.of(kind, syms));
        }

        Set<String> srcKeys = srcMap.keySet();
        return buildInternal(raw, srcKeys, srcMap, tgtMap);
    }

    private static Constraint parseConstraint(ConstraintSegment cs,
                                               Map<String, Symbol> srcMap,
                                               Map<String, Symbol> tgtMap,
                                               boolean isMatchConstraint) {
        ConstraintKind kind = ConstraintKind.fromShardingSphere(cs.getType().name());
        String[] params = cs.getParams();
        Symbol[] syms = new Symbol[params.length];
        for (int i = 0; i < params.length; i++) {
            Symbol sym = srcMap.get(params[i]);
            if (sym == null) {
                sym = tgtMap.get(params[i]);
            }
            if (sym == null && SymbolKind.isSymbolName(params[i])) {
                sym = Symbol.of(params[i]);
                if (isMatchConstraint) {
                    // Match constraints are source-side only
                    srcMap.put(params[i], sym);
                } else if (kind.isEq() && i == 1) {
                    // For rewrite eq constraints, second param is source
                    srcMap.put(params[i], sym);
                } else {
                    // For rewrite eq constraints first param, or non-eq, assume target
                    tgtMap.put(params[i], sym);
                }
            }
            if (sym == null) {
                throw new IllegalArgumentException(
                        "Symbol not found for param: " + params[i]);
            }
            syms[i] = sym;
        }
        return Constraint.of(kind, syms);
    }

    private static Symbol resolveOrCreateSymbol(String param,
                                                 Map<String, Symbol> srcMap,
                                                 Map<String, Symbol> tgtMap) {
        Symbol sym = srcMap.get(param);
        if (sym == null) {
            sym = tgtMap.get(param);
        }
        if (sym == null && SymbolKind.isSymbolName(param)) {
            sym = Symbol.of(param);
            // Heuristic: put in whichever side doesn't have a symbol of this kind
            if (hasSymbolWithPrefix(srcMap, param.charAt(0))
                    && !hasSymbolWithPrefix(tgtMap, param.charAt(0))) {
                tgtMap.put(param, sym);
            } else {
                srcMap.put(param, sym);
            }
        }
        if (sym == null) {
            throw new IllegalArgumentException(
                    "Symbol not found for param: " + param);
        }
        return sym;
    }

    private static boolean hasSymbolWithPrefix(Map<String, Symbol> symbols, char prefix) {
        for (String name : symbols.keySet()) {
            if (name.charAt(0) == prefix) {
                return true;
            }
        }
        return false;
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
