package org.autorewriter.rewriter.rule.constraint;

import org.autorewriter.rewriter.rule.symbol.Symbol;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Constraints}, {@link Constraint}, and {@link ConstraintKind}.
 */
class ConstraintsTest {

    // ── Helper: create symbol sets ──────────────────────────────────────

    private static Set<Symbol> setOf(Symbol... symbols) {
        return new HashSet<>(Arrays.asList(symbols));
    }

    // ── testEqClassBuilding ─────────────────────────────────────────────

    @Test
    void testEqClassBuilding() {
        Symbol t0 = Symbol.of("t0");
        Symbol t1 = Symbol.of("t1");

        Constraint eq = Constraint.of(ConstraintKind.TABLE_EQ, t0, t1);

        Constraints constraints = Constraints.build(
                Arrays.asList(eq),
                setOf(t0, t1),   // both source
                setOf()          // no target
        );

        assertTrue(constraints.isEq(t0, t1));
        assertTrue(constraints.isEq(t1, t0));

        Set<Symbol> eqClass = constraints.eqClassOf(t0);
        assertEquals(2, eqClass.size());
        assertTrue(eqClass.contains(t0));
        assertTrue(eqClass.contains(t1));

        // The TABLE_EQ constraint should remain in the main list (both source-side)
        assertEquals(1, constraints.size());
    }

    // ── testInstantiationMapping ────────────────────────────────────────

    @Test
    void testInstantiationMapping() {
        Symbol t0 = Symbol.of("t0");  // source
        Symbol t2 = Symbol.of("t2");  // target

        Constraint crossEq = Constraint.of(ConstraintKind.TABLE_EQ, t2, t0);

        Constraints constraints = Constraints.build(
                Arrays.asList(crossEq),
                setOf(t0),   // source
                setOf(t2)    // target
        );

        // Cross-side equality captured in instantiation map
        assertEquals(t0, constraints.instantiationOf(t2));

        // Cross-side constraints removed from main list
        assertEquals(0, constraints.size());

        // t0 and t2 should NOT be in the same eq class (cross-side)
        assertFalse(constraints.isEq(t0, t2));
    }

    // ── testOfKind ──────────────────────────────────────────────────────

    @Test
    void testOfKind() {
        Symbol t0 = Symbol.of("t0");
        Symbol t1 = Symbol.of("t1");
        Symbol a0 = Symbol.of("a0");
        Symbol a1 = Symbol.of("a1");
        Symbol p0 = Symbol.of("p0");
        Symbol p1 = Symbol.of("p1");

        Constraint tableEq = Constraint.of(ConstraintKind.TABLE_EQ, t0, t1);
        Constraint attrsEq = Constraint.of(ConstraintKind.ATTRS_EQ, a0, a1);
        Constraint attrsSub = Constraint.of(ConstraintKind.ATTRS_SUB, a0, t0);
        Constraint unique = Constraint.of(ConstraintKind.UNIQUE, t0, a0);

        Set<Symbol> source = setOf(t0, t1, a0, a1, p0, p1);

        Constraints constraints = Constraints.build(
                Arrays.asList(tableEq, attrsEq, attrsSub, unique),
                source,
                setOf()
        );

        // TABLE_EQ
        List<Constraint> tableEqs = constraints.ofKind(ConstraintKind.TABLE_EQ);
        assertEquals(1, tableEqs.size());
        assertEquals(ConstraintKind.TABLE_EQ, tableEqs.get(0).kind());

        // ATTRS_EQ
        List<Constraint> attrsEqs = constraints.ofKind(ConstraintKind.ATTRS_EQ);
        assertEquals(1, attrsEqs.size());
        assertEquals(ConstraintKind.ATTRS_EQ, attrsEqs.get(0).kind());

        // ATTRS_SUB
        List<Constraint> attrsSubs = constraints.ofKind(ConstraintKind.ATTRS_SUB);
        assertEquals(1, attrsSubs.size());
        assertEquals(ConstraintKind.ATTRS_SUB, attrsSubs.get(0).kind());

        // UNIQUE
        List<Constraint> uniques = constraints.ofKind(ConstraintKind.UNIQUE);
        assertEquals(1, uniques.size());
        assertEquals(ConstraintKind.UNIQUE, uniques.get(0).kind());

        // PREDICATE_EQ — none added
        List<Constraint> predEqs = constraints.ofKind(ConstraintKind.PREDICATE_EQ);
        assertTrue(predEqs.isEmpty());

        // Total: all 4 remain (all source-side)
        assertEquals(4, constraints.size());
    }

    // ── testSourceOf ────────────────────────────────────────────────────

    @Test
    void testSourceOf() {
        Symbol a0 = Symbol.of("a0");
        Symbol t0 = Symbol.of("t0");

        Constraint attrsSub = Constraint.of(ConstraintKind.ATTRS_SUB, a0, t0);

        Constraints constraints = Constraints.build(
                Arrays.asList(attrsSub),
                setOf(a0, t0),
                setOf()
        );

        assertEquals(t0, constraints.sourceOf(a0));
        assertNull(constraints.sourceOf(t0)); // t0 is not the attrs symbol in any ATTRS_SUB
    }

    // ── testAttrsSub ────────────────────────────────────────────────────

    @Test
    void testAttrsSub() {
        Symbol a0 = Symbol.of("a0");
        Symbol t0 = Symbol.of("t0");
        Symbol a1 = Symbol.of("a1");
        Symbol t1 = Symbol.of("t1");

        Constraint sub0 = Constraint.of(ConstraintKind.ATTRS_SUB, a0, t0);
        Constraint sub1 = Constraint.of(ConstraintKind.ATTRS_SUB, a1, t1);

        Constraints constraints = Constraints.build(
                Arrays.asList(sub0, sub1),
                setOf(a0, t0, a1, t1),
                setOf()
        );

        // Both ATTRS_SUB constraints should be preserved in main list
        List<Constraint> subs = constraints.ofKind(ConstraintKind.ATTRS_SUB);
        assertEquals(2, subs.size());
        assertEquals(2, constraints.size());

        // sourceOf should work for both
        assertEquals(t0, constraints.sourceOf(a0));
        assertEquals(t1, constraints.sourceOf(a1));
    }

    // ── testConstraintKindProperties ────────────────────────────────────

    @Test
    void testConstraintKindProperties() {
        assertTrue(ConstraintKind.TABLE_EQ.isEq());
        assertTrue(ConstraintKind.ATTRS_EQ.isEq());
        assertTrue(ConstraintKind.PREDICATE_EQ.isEq());
        assertTrue(ConstraintKind.SCHEMA_EQ.isEq());
        assertFalse(ConstraintKind.ATTRS_SUB.isEq());
        assertFalse(ConstraintKind.UNIQUE.isEq());

        assertTrue(ConstraintKind.UNIQUE.isIntegrityConstraint());
        assertTrue(ConstraintKind.NOT_NULL.isIntegrityConstraint());
        assertTrue(ConstraintKind.REFERENCE.isIntegrityConstraint());
        assertFalse(ConstraintKind.TABLE_EQ.isIntegrityConstraint());

        assertEquals(2, ConstraintKind.TABLE_EQ.numSymbols());
        assertEquals(4, ConstraintKind.REFERENCE.numSymbols());
    }

    // ── testConstraintKindFromShardingSphere ─────────────────────────────

    @Test
    void testConstraintKindFromShardingSphere() {
        assertEquals(ConstraintKind.TABLE_EQ, ConstraintKind.fromShardingSphere("TABLE_EQ"));
        assertEquals(ConstraintKind.REFERENCE, ConstraintKind.fromShardingSphere("REFERENCE"));
        assertThrows(IllegalArgumentException.class, () -> ConstraintKind.fromShardingSphere("UNKNOWN"));
    }

    // ── testConstraintToString ──────────────────────────────────────────

    @Test
    void testConstraintToString() {
        Symbol t0 = Symbol.of("t0");
        Symbol t1 = Symbol.of("t1");
        Constraint c = Constraint.of(ConstraintKind.TABLE_EQ, t0, t1);

        assertEquals("TABLE_EQ(t0, t1)", c.toString());
    }

    // ── testConstraintEquality ──────────────────────────────────────────

    @Test
    void testConstraintEquality() {
        Symbol t0 = Symbol.of("t0");
        Symbol t1 = Symbol.of("t1");

        Constraint c1 = Constraint.of(ConstraintKind.TABLE_EQ, t0, t1);
        Constraint c2 = Constraint.of(ConstraintKind.TABLE_EQ, t0, t1);
        Constraint c3 = Constraint.of(ConstraintKind.ATTRS_EQ, t0, t1);

        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    // ── testConstraintSymbolCountValidation ─────────────────────────────

    @Test
    void testConstraintSymbolCountValidation() {
        Symbol t0 = Symbol.of("t0");

        // TABLE_EQ expects 2 symbols, not 1
        assertThrows(IllegalArgumentException.class, () -> Constraint.of(ConstraintKind.TABLE_EQ, t0));
    }

    // ── testMixedCrossSideAndSameSide ───────────────────────────────────

    @Test
    void testMixedCrossSideAndSameSide() {
        Symbol t0 = Symbol.of("t0");  // source
        Symbol t1 = Symbol.of("t1");  // source
        Symbol t2 = Symbol.of("t2");  // target
        Symbol a0 = Symbol.of("a0");  // source

        // t0 == t1 (both source) → eq class
        // t2 == t0 (cross-side) → instantiation
        // ATTRS_SUB(a0, t0) → kept
        Constraint sameSideEq = Constraint.of(ConstraintKind.TABLE_EQ, t0, t1);
        Constraint crossSideEq = Constraint.of(ConstraintKind.TABLE_EQ, t2, t0);
        Constraint attrsSub = Constraint.of(ConstraintKind.ATTRS_SUB, a0, t0);

        Constraints constraints = Constraints.build(
                Arrays.asList(sameSideEq, crossSideEq, attrsSub),
                setOf(t0, t1, a0),  // source
                setOf(t2)           // target
        );

        // Same-side eq class
        assertTrue(constraints.isEq(t0, t1));

        // Cross-side instantiation
        assertEquals(t0, constraints.instantiationOf(t2));

        // Only sameSideEq and attrsSub should remain (cross-side removed)
        assertEquals(2, constraints.size());

        // ATTRS_SUB preserved
        assertEquals(t0, constraints.sourceOf(a0));
    }
}
