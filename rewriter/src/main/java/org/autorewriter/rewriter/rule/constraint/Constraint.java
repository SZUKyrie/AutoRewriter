package org.autorewriter.rewriter.rule.constraint;

import org.autorewriter.rewriter.rule.symbol.Symbol;

import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable constraint that relates a fixed number of {@link Symbol} parameters
 * under a given {@link ConstraintKind}.
 *
 * <p>Examples: {@code TABLE_EQ(t0, t1)}, {@code ATTRS_SUB(a0, t0)},
 * {@code REFERENCE(t0, a0, t1, a1)}.
 */
public final class Constraint {

    private final ConstraintKind kind;
    private final Symbol[] symbols;

    private Constraint(ConstraintKind kind, Symbol[] symbols) {
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(symbols, "symbols must not be null");
        if (symbols.length != kind.numSymbols()) {
            throw new IllegalArgumentException(
                    kind + " expects " + kind.numSymbols() + " symbols but got " + symbols.length);
        }
        // Defensive copy
        this.symbols = symbols.clone();
    }

    /**
     * Creates a constraint of the given kind with the specified symbols.
     *
     * @param kind    the constraint kind
     * @param symbols the symbol parameters (length must match {@code kind.numSymbols()})
     * @return a new immutable {@code Constraint}
     * @throws IllegalArgumentException if the symbol count does not match the kind
     */
    public static Constraint of(ConstraintKind kind, Symbol... symbols) {
        return new Constraint(kind, symbols);
    }

    /** Returns the kind of this constraint. */
    public ConstraintKind kind() {
        return kind;
    }

    /** Returns a defensive copy of the symbol parameters. */
    public Symbol[] symbols() {
        return symbols.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Constraint)) return false;
        Constraint that = (Constraint) o;
        return kind == that.kind && Arrays.equals(symbols, that.symbols);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(kind);
        result = 31 * result + Arrays.hashCode(symbols);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(kind.name()).append('(');
        for (int i = 0; i < symbols.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(symbols[i]);
        }
        sb.append(')');
        return sb.toString();
    }
}
