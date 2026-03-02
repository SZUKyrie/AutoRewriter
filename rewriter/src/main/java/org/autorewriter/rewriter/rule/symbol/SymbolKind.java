package org.autorewriter.rewriter.rule.symbol;

/**
 * Enumerates the kinds of symbols used in WeTune-style template matching.
 *
 * <p>Each kind corresponds to a single-character prefix used in placeholder names:
 * <ul>
 *   <li>{@code t} — TABLE: binds to a RelNode (query sub-tree)</li>
 *   <li>{@code a} — ATTRS: binds to a list of column references</li>
 *   <li>{@code p} — PRED: binds to a RexNode (predicate expression)</li>
 *   <li>{@code s} — SCHEMA: binds to a RelDataType (output schema)</li>
 * </ul>
 */
public enum SymbolKind {
    TABLE,   // t0, t1, ... → binds to RelNode (query subtree)
    ATTRS,   // a0, a1, ... → binds to List<ColumnRef> (column references)
    PRED,    // p0, p1, ... → binds to RexNode (predicate expression)
    SCHEMA;  // s0, s1, ... → binds to RelDataType (output schema)

    /**
     * Resolve the {@link SymbolKind} that corresponds to the given single-character prefix.
     *
     * @param prefix one of {@code 't'}, {@code 'a'}, {@code 'p'}, {@code 's'}
     * @return the matching kind
     * @throws IllegalArgumentException if the prefix is not recognised
     */
    public static SymbolKind fromPrefix(char prefix) {
        switch (prefix) {
            case 't': return TABLE;
            case 'a': return ATTRS;
            case 'p': return PRED;
            case 's': return SCHEMA;
            default: throw new IllegalArgumentException("Unknown symbol prefix: " + prefix);
        }
    }

    /**
     * Returns {@code true} when {@code name} matches the pattern {@code [tasp]\d+},
     * i.e. it is a valid placeholder name such as {@code t0}, {@code a1}, {@code p2}, {@code s0}.
     */
    public static boolean isSymbolName(String name) {
        if (name == null || name.length() < 2) {
            return false;
        }
        char prefix = name.charAt(0);
        if (prefix != 't' && prefix != 'a' && prefix != 'p' && prefix != 's') {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isDigit(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
