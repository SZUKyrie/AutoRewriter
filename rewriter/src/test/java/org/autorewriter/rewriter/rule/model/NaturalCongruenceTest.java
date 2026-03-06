package org.autorewriter.rewriter.rule.model;

import org.autorewriter.rewriter.rule.symbol.Symbol;
import org.autorewriter.rewriter.rule.symbol.SymbolKind;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NaturalCongruence}.
 */
class NaturalCongruenceTest {

    // ── testBasicCongruence ─────────────────────────────────────────────

    @Test
    void testBasicCongruence() {
        NaturalCongruence<String> cong = NaturalCongruence.create();
        cong.putCongruent("a", "b");

        assertTrue(cong.isCongruent("a", "b"));
        assertTrue(cong.isCongruent("b", "a"));
        assertFalse(cong.isCongruent("a", "c"));
        assertFalse(cong.isCongruent("b", "c"));
    }

    // ── testTransitivity ────────────────────────────────────────────────

    @Test
    void testTransitivity() {
        NaturalCongruence<String> cong = NaturalCongruence.create();
        cong.putCongruent("a", "b");
        cong.putCongruent("b", "c");

        assertTrue(cong.isCongruent("a", "c"));
        assertTrue(cong.isCongruent("c", "a"));
        assertTrue(cong.isCongruent("a", "b"));
        assertTrue(cong.isCongruent("b", "c"));
    }

    // ── testMergeExistingClasses ────────────────────────────────────────

    @Test
    void testMergeExistingClasses() {
        NaturalCongruence<String> cong = NaturalCongruence.create();
        // Create two separate classes
        cong.putCongruent("a", "b");
        cong.putCongruent("c", "d");

        // Verify they are separate
        assertFalse(cong.isCongruent("a", "c"));
        assertFalse(cong.isCongruent("b", "d"));

        // Merge via bridge element
        cong.putCongruent("b", "c");

        // Now all four should be congruent
        assertTrue(cong.isCongruent("a", "b"));
        assertTrue(cong.isCongruent("a", "c"));
        assertTrue(cong.isCongruent("a", "d"));
        assertTrue(cong.isCongruent("b", "c"));
        assertTrue(cong.isCongruent("b", "d"));
        assertTrue(cong.isCongruent("c", "d"));
    }

    // ── testReflexivity ─────────────────────────────────────────────────

    @Test
    void testReflexivity() {
        NaturalCongruence<String> cong = NaturalCongruence.create();

        // Reflexive even without registration
        assertTrue(cong.isCongruent("a", "a"));
        assertTrue(cong.isCongruent("z", "z"));

        // Also reflexive after registration
        cong.mkEqClass("x");
        assertTrue(cong.isCongruent("x", "x"));
    }

    // ── testEqClassOf ───────────────────────────────────────────────────

    @Test
    void testEqClassOf() {
        NaturalCongruence<String> cong = NaturalCongruence.create();
        cong.putCongruent("a", "b");
        cong.putCongruent("b", "c");

        Set<String> eqClass = cong.eqClassOf("a");
        assertEquals(3, eqClass.size());
        assertTrue(eqClass.contains("a"));
        assertTrue(eqClass.contains("b"));
        assertTrue(eqClass.contains("c"));

        // All three should return the same set (identity)
        assertSame(cong.eqClassOf("a"), cong.eqClassOf("b"));
        assertSame(cong.eqClassOf("b"), cong.eqClassOf("c"));
    }

    // ── testMkEqClass ───────────────────────────────────────────────────

    @Test
    void testMkEqClass() {
        NaturalCongruence<String> cong = NaturalCongruence.create();

        Set<String> cls1 = cong.mkEqClass("x");
        assertNotNull(cls1);
        assertEquals(1, cls1.size());
        assertTrue(cls1.contains("x"));

        // Second call returns the same set instance
        Set<String> cls2 = cong.mkEqClass("x");
        assertSame(cls1, cls2);
    }

    // ── testUnregisteredElement ─────────────────────────────────────────

    @Test
    void testUnregisteredElement() {
        NaturalCongruence<String> cong = NaturalCongruence.create();

        Set<String> eqClass = cong.eqClassOf("unregistered");
        assertEquals(1, eqClass.size());
        assertTrue(eqClass.contains("unregistered"));
    }

    // ── testWithSymbols ─────────────────────────────────────────────────

    @Test
    void testWithSymbols() {
        NaturalCongruence<Symbol> cong = NaturalCongruence.create();

        Symbol t0 = Symbol.of("t0");
        Symbol t1 = Symbol.of("t1");
        Symbol a0 = Symbol.of("a0");
        Symbol a1 = Symbol.of("a1");

        cong.putCongruent(t0, t1);
        cong.putCongruent(a0, a1);

        assertTrue(cong.isCongruent(t0, t1));
        assertTrue(cong.isCongruent(a0, a1));
        assertFalse(cong.isCongruent(t0, a0));

        Set<Symbol> tableClass = cong.eqClassOf(t0);
        assertEquals(2, tableClass.size());
        assertTrue(tableClass.contains(t0));
        assertTrue(tableClass.contains(t1));

        // Verify Symbol.of creates equal objects that work with the map
        Symbol t0Copy = Symbol.of(SymbolKind.TABLE, "t0");
        assertTrue(cong.isCongruent(t0, t0Copy));
        assertSame(cong.eqClassOf(t0), cong.eqClassOf(t0Copy));
    }
}
