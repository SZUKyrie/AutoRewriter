package org.autorewriter.rewriter.rule.symbol;

import java.util.Objects;

/**
 * An immutable symbol that represents a placeholder in a WeTune-style rewrite template.
 *
 * <p>A symbol has a {@link SymbolKind} (TABLE, ATTRS, PRED, SCHEMA) and a name
 * such as {@code t0}, {@code a1}, {@code p2} or {@code s0}. Symbols are used as
 * keys in binding maps during template matching and instantiation.
 */
public final class Symbol {

    private final SymbolKind kind;
    private final String name;

    private Symbol(SymbolKind kind, String name) {
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Creates a symbol with an explicit kind and name.
     *
     * @param kind the symbol kind
     * @param name the placeholder name (e.g. {@code "t0"})
     * @return a new Symbol
     */
    public static Symbol of(SymbolKind kind, String name) {
        return new Symbol(kind, name);
    }

    /**
     * Creates a symbol by inferring its kind from the first character of {@code name}.
     *
     * @param name a valid placeholder name matching {@code [tasp]\d+}
     * @return a new Symbol
     * @throws IllegalArgumentException if the name is not a valid symbol name
     */
    public static Symbol of(String name) {
        if (!SymbolKind.isSymbolName(name)) {
            throw new IllegalArgumentException("Not a valid symbol name: " + name);
        }
        return new Symbol(SymbolKind.fromPrefix(name.charAt(0)), name);
    }

    /** Returns the kind of this symbol. */
    public SymbolKind kind() {
        return kind;
    }

    /** Returns the name of this symbol (e.g. {@code "t0"}). */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Symbol)) return false;
        Symbol s = (Symbol) o;
        return kind == s.kind && name.equals(s.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, name);
    }

    @Override
    public String toString() {
        return name;
    }
}
