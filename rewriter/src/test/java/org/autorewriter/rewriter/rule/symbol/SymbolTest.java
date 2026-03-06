package org.autorewriter.rewriter.rule.symbol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Symbol} and {@link SymbolKind}.
 */
class SymbolTest {

    // ── SymbolKind.fromPrefix ───────────────────────────────────────────

    @Test
    void fromPrefix_table() {
        assertEquals(SymbolKind.TABLE, SymbolKind.fromPrefix('t'));
    }

    @Test
    void fromPrefix_attrs() {
        assertEquals(SymbolKind.ATTRS, SymbolKind.fromPrefix('a'));
    }

    @Test
    void fromPrefix_pred() {
        assertEquals(SymbolKind.PRED, SymbolKind.fromPrefix('p'));
    }

    @Test
    void fromPrefix_schema() {
        assertEquals(SymbolKind.SCHEMA, SymbolKind.fromPrefix('s'));
    }

    @Test
    void fromPrefix_unknown_throws() {
        assertThrows(IllegalArgumentException.class, () -> SymbolKind.fromPrefix('x'));
    }

    // ── SymbolKind.isSymbolName ─────────────────────────────────────────

    @Test
    void isSymbolName_valid_names() {
        assertTrue(SymbolKind.isSymbolName("t0"));
        assertTrue(SymbolKind.isSymbolName("a1"));
        assertTrue(SymbolKind.isSymbolName("p2"));
        assertTrue(SymbolKind.isSymbolName("s3"));
        assertTrue(SymbolKind.isSymbolName("t10"));
        assertTrue(SymbolKind.isSymbolName("a99"));
    }

    @Test
    void isSymbolName_invalid_names() {
        assertFalse(SymbolKind.isSymbolName(null));
        assertFalse(SymbolKind.isSymbolName(""));
        assertFalse(SymbolKind.isSymbolName("t"));      // too short, no digit
        assertFalse(SymbolKind.isSymbolName("x0"));      // wrong prefix
        assertFalse(SymbolKind.isSymbolName("T0"));      // uppercase
        assertFalse(SymbolKind.isSymbolName("table"));   // not a symbol
        assertFalse(SymbolKind.isSymbolName("0t"));      // digit first
        assertFalse(SymbolKind.isSymbolName("a0x"));     // trailing non-digit
    }

    // ── Symbol.of(kind, name) ───────────────────────────────────────────

    @Test
    void of_kindAndName() {
        Symbol s = Symbol.of(SymbolKind.TABLE, "t0");
        assertEquals(SymbolKind.TABLE, s.kind());
        assertEquals("t0", s.name());
    }

    @Test
    void of_kindAndName_nullKind_throws() {
        assertThrows(NullPointerException.class, () -> Symbol.of(null, "t0"));
    }

    @Test
    void of_kindAndName_nullName_throws() {
        assertThrows(NullPointerException.class, () -> Symbol.of(SymbolKind.TABLE, null));
    }

    // ── Symbol.of(name) ─────────────────────────────────────────────────

    @Test
    void of_name_table() {
        Symbol s = Symbol.of("t0");
        assertEquals(SymbolKind.TABLE, s.kind());
        assertEquals("t0", s.name());
    }

    @Test
    void of_name_attrs() {
        Symbol s = Symbol.of("a1");
        assertEquals(SymbolKind.ATTRS, s.kind());
        assertEquals("a1", s.name());
    }

    @Test
    void of_name_pred() {
        Symbol s = Symbol.of("p2");
        assertEquals(SymbolKind.PRED, s.kind());
        assertEquals("p2", s.name());
    }

    @Test
    void of_name_schema() {
        Symbol s = Symbol.of("s3");
        assertEquals(SymbolKind.SCHEMA, s.kind());
        assertEquals("s3", s.name());
    }

    @Test
    void of_name_multiDigit() {
        Symbol s = Symbol.of("t12");
        assertEquals(SymbolKind.TABLE, s.kind());
        assertEquals("t12", s.name());
    }

    @Test
    void of_name_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> Symbol.of("invalid"));
        assertThrows(IllegalArgumentException.class, () -> Symbol.of("x0"));
        assertThrows(IllegalArgumentException.class, () -> Symbol.of(""));
    }

    // ── equals / hashCode ───────────────────────────────────────────────

    @Test
    void equals_sameKindAndName() {
        Symbol a = Symbol.of("t0");
        Symbol b = Symbol.of(SymbolKind.TABLE, "t0");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_differentName() {
        Symbol a = Symbol.of("t0");
        Symbol b = Symbol.of("t1");
        assertNotEquals(a, b);
    }

    @Test
    void equals_differentKind_sameName() {
        // Using the two-arg factory to create a symbol with a mismatched kind
        Symbol a = Symbol.of(SymbolKind.TABLE, "t0");
        Symbol b = Symbol.of(SymbolKind.ATTRS, "t0");
        assertNotEquals(a, b);
    }

    @Test
    void equals_null_and_otherType() {
        Symbol s = Symbol.of("t0");
        assertNotEquals(s, null);
        assertNotEquals(s, "t0");
    }

    @Test
    void equals_reflexive() {
        Symbol s = Symbol.of("a1");
        assertEquals(s, s);
    }

    // ── toString ────────────────────────────────────────────────────────

    @Test
    void toString_returnsName() {
        assertEquals("t0", Symbol.of("t0").toString());
        assertEquals("p5", Symbol.of("p5").toString());
    }
}
