package org.autorewriter.rewriter.rule.constraint;

/**
 * Enumerates the kinds of constraints used in WeTune-style rule matching.
 *
 * <p>Each kind declares how many symbol parameters it expects:
 * <ul>
 *   <li>Equality constraints ({@link #TABLE_EQ}, {@link #ATTRS_EQ}, {@link #PREDICATE_EQ},
 *       {@link #SCHEMA_EQ}) — 2 symbols</li>
 *   <li>{@link #ATTRS_SUB} — 2 symbols (attrs, table)</li>
 *   <li>Integrity constraints ({@link #UNIQUE}, {@link #NOT_NULL}) — 2 symbols</li>
 *   <li>{@link #REFERENCE} — 4 symbols</li>
 * </ul>
 */
public enum ConstraintKind {
    TABLE_EQ(2),
    ATTRS_EQ(2),
    PREDICATE_EQ(2),
    SCHEMA_EQ(2),
    ATTRS_SUB(2),
    UNIQUE(2),
    NOT_NULL(2),
    REFERENCE(4);

    private final int numSymbols;

    ConstraintKind(int n) {
        this.numSymbols = n;
    }

    /** Returns the number of symbol parameters this constraint kind expects. */
    public int numSymbols() {
        return numSymbols;
    }

    /** Returns {@code true} if this kind is an equality constraint. */
    public boolean isEq() {
        return this == TABLE_EQ || this == ATTRS_EQ || this == PREDICATE_EQ || this == SCHEMA_EQ;
    }

    /** Returns {@code true} if this kind is an integrity constraint (UNIQUE, NOT_NULL, REFERENCE). */
    public boolean isIntegrityConstraint() {
        return this == UNIQUE || this == NOT_NULL || this == REFERENCE;
    }

    /**
     * Maps from a ShardingSphere {@code ConstraintType} enum name to our {@link ConstraintKind}.
     *
     * @param typeName the {@code name()} of a ShardingSphere ConstraintType
     * @return the corresponding {@code ConstraintKind}
     * @throws IllegalArgumentException if the name is not recognised
     */
    public static ConstraintKind fromShardingSphere(String typeName) {
        switch (typeName) {
            case "TABLE_EQ":     return TABLE_EQ;
            case "ATTRS_EQ":     return ATTRS_EQ;
            case "PREDICATE_EQ": return PREDICATE_EQ;
            case "SCHEMA_EQ":    return SCHEMA_EQ;
            case "ATTRS_SUB":    return ATTRS_SUB;
            case "UNIQUE":       return UNIQUE;
            case "NOT_NULL":     return NOT_NULL;
            case "REFERENCE":    return REFERENCE;
            default:
                throw new IllegalArgumentException("Unknown constraint type: " + typeName);
        }
    }
}
