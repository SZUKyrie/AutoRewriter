package org.autorewriter.rewriter.rule.model;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalTableScan;
import org.apache.calcite.rel.metadata.DefaultRelMetadataProvider;
import org.apache.calcite.rel.metadata.JaninoRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.util.ImmutableBitSet;
import org.autorewriter.rewriter.rule.constraint.Constraint;
import org.autorewriter.rewriter.rule.constraint.Constraints;
import org.autorewriter.rewriter.rule.symbol.Symbol;
import org.autorewriter.rewriter.rule.symbol.SymbolKind;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.autorewriter.rewriter.rule.util.ColumnRefResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Symbol-to-value binding container with Union-Find consistency checking
 * and copy-on-write {@link #derive()} for backtracking.
 *
 * <p>Ported from WeTune's {@code Model} class but adapted for Calcite
 * {@link RelNode} (not WeTune's custom PlanContext).
 *
 * <p>A Model maintains a map from {@link Symbol} keys to concrete values
 * (RelNode, List&lt;ColumnRef&gt;, RexNode, RelDataType). When a value is
 * {@linkplain #assign(Symbol, Object) assigned}, equivalence-class consistency
 * is checked against the {@link Constraints} that were supplied at construction.
 *
 * <p>The {@link #derive()} method creates a lightweight child model that
 * overlays its own assignments on top of the parent's, enabling efficient
 * backtracking: simply discard the child to undo all trial assignments.
 */
public class Model {

    private static final JaninoRelMetadataProvider METADATA_PROVIDER =
            JaninoRelMetadataProvider.of(DefaultRelMetadataProvider.INSTANCE);

    private final Model base;           // parent model for derive chain, null for root
    private final Constraints constraints;
    private Map<Symbol, Object> assignments;  // lazy init

    // ── Constructors ─────────────────────────────────────────────────────

    /** Root constructor. */
    public Model(Constraints constraints) {
        this.base = null;
        this.constraints = constraints;
        this.assignments = null; // lazy
    }

    /** Derive constructor (private). */
    private Model(Model parent) {
        this.base = parent;
        this.constraints = parent.constraints;
        this.assignments = null; // lazy
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    /**
     * Creates a child model (copy-on-write snapshot for backtracking).
     * The child sees all parent assignments but can add its own without
     * affecting the parent.
     */
    public Model derive() {
        return new Model(this);
    }

    /** Clears all assignments in this model (does not affect parent). */
    public void reset() {
        if (assignments != null) assignments.clear();
    }

    /** Returns the constraints associated with this model. */
    public Constraints constraints() {
        return constraints;
    }

    // ── Typed accessors ──────────────────────────────────────────────────

    /** Returns the RelNode bound to a TABLE symbol, or {@code null}. */
    public RelNode ofTable(Symbol sym) { return of(sym); }

    /** Returns the column-ref list bound to an ATTRS symbol, or {@code null}. */
    @SuppressWarnings("unchecked")
    public List<ColumnRef> ofAttrs(Symbol sym) { return of(sym); }

    /** Returns the RexNode bound to a PRED symbol, or {@code null}. */
    public RexNode ofPred(Symbol sym) { return of(sym); }

    /** Returns the RelDataType bound to a SCHEMA symbol, or {@code null}. */
    public RelDataType ofSchema(Symbol sym) { return of(sym); }

    /** Returns extra data stored under a synthetic symbol key. */
    public Object ofExtra(String key) { return of(Symbol.of(SymbolKind.ATTRS, key)); }

    /** Returns {@code true} if the given symbol has been assigned a value. */
    public boolean isAssigned(Symbol sym) { return of(sym) != null; }

    // ── Core assign method ───────────────────────────────────────────────

    /**
     * Assigns a value to the given symbol and checks equivalence-class consistency.
     *
     * <p>If the symbol belongs to an equivalence class that already has a value
     * assigned for another member, compatibility is checked. Returns {@code false}
     * if the assignment is inconsistent.
     *
     * @param sym   the symbol to assign
     * @param value the value to bind
     * @return {@code true} if the assignment is consistent, {@code false} otherwise
     */
    public boolean assign(Symbol sym, Object value) {
        ensureAssignments();
        assignments.put(sym, value);

        // Check consistency with all equivalent symbols
        for (Symbol eqSym : constraints.eqClassOf(sym)) {
            if (eqSym.equals(sym)) continue;
            Object existing = of(eqSym);
            if (existing != null && !checkCompatible(sym.kind(), value, existing)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Stores an extra binding (e.g. projection RexNodes) without compatibility check.
     *
     * @param key   the key (used to create a synthetic ATTRS symbol)
     * @param value the value to store
     */
    public void putExtra(String key, Object value) {
        ensureAssignments();
        assignments.put(Symbol.of(SymbolKind.ATTRS, key), value);
    }

    // ── Constraint checking ──────────────────────────────────────────────

    /**
     * Checks all constraints against current assignments.
     * Unassigned symbols are skipped (constraints are only validated
     * when all their operands are bound).
     *
     * @return {@code true} if all constraints are satisfied
     */
    public boolean checkConstraints() {
        for (Constraint c : constraints) {
            if (!checkConstraint(c)) return false;
        }
        return true;
    }

    // ── Private helpers ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> T of(Symbol sym) {
        if (assignments != null) {
            Object val = assignments.get(sym);
            if (val != null) return (T) val;
        }
        if (base != null) return base.of(sym);
        return null;
    }

    private void ensureAssignments() {
        if (assignments == null) assignments = new HashMap<>();
    }

    private boolean checkCompatible(SymbolKind kind, Object v0, Object v1) {
        switch (kind) {
            case TABLE:
                return checkTableCompatible(v0, v1);
            case ATTRS:
                return checkAttrsCompatible(v0, v1);
            case PRED:
                return checkPredCompatible(v0, v1);
            case SCHEMA:
                return true; // schema always compatible (validated later)
            default:
                return false;
        }
    }

    private boolean checkTableCompatible(Object v0, Object v1) {
        if (!(v0 instanceof RelNode) || !(v1 instanceof RelNode)) return false;
        RelNode r0 = (RelNode) v0;
        RelNode r1 = (RelNode) v1;
        // For LogicalTableScan, compare qualified names
        if (r0 instanceof LogicalTableScan && r1 instanceof LogicalTableScan) {
            return ((LogicalTableScan) r0).getTable().getQualifiedName()
                    .equals(((LogicalTableScan) r1).getTable().getQualifiedName());
        }
        // For other RelNodes, use deepEquals
        return r0.deepEquals(r1);
    }

    @SuppressWarnings("unchecked")
    private boolean checkAttrsCompatible(Object v0, Object v1) {
        if (!(v0 instanceof List) || !(v1 instanceof List)) return false;
        List<ColumnRef> a0 = (List<ColumnRef>) v0;
        List<ColumnRef> a1 = (List<ColumnRef>) v1;
        return a0.equals(a1);
    }

    private boolean checkPredCompatible(Object v0, Object v1) {
        if (!(v0 instanceof RexNode) || !(v1 instanceof RexNode)) return false;
        return v0.toString().equals(v1.toString());
    }

    private boolean checkConstraint(Constraint c) {
        switch (c.kind()) {
            case ATTRS_SUB: return checkAttrsSub(c);
            case UNIQUE:    return checkUnique(c);
            case NOT_NULL:  return checkNotNull(c);
            case REFERENCE: return checkReference(c);
            default:        return true; // equality constraints handled by assign()
        }
    }

    /**
     * ATTRS_SUB(attrs, table): attrs are a subset of table's output columns.
     */
    @SuppressWarnings("unchecked")
    private boolean checkAttrsSub(Constraint c) {
        Symbol attrsSym = c.symbols()[0];
        Symbol tableSym = c.symbols()[1];
        List<ColumnRef> attrs = ofAttrs(attrsSym);
        if (attrs == null) return true; // not assigned yet
        RelNode tableNode = ofTable(tableSym);
        if (tableNode == null) return true; // not assigned yet

        List<String> fieldNames = tableNode.getRowType().getFieldNames();
        for (ColumnRef ref : attrs) {
            boolean found = false;
            for (String fieldName : fieldNames) {
                if (fieldName.equalsIgnoreCase(ref.getColumnName())) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    /**
     * UNIQUE(table, attrs): attrs form a unique key on table.
     */
    @SuppressWarnings("unchecked")
    private boolean checkUnique(Constraint c) {
        Symbol tableSym = c.symbols()[0];
        Symbol attrsSym = c.symbols()[1];
        List<ColumnRef> attrs = ofAttrs(attrsSym);
        if (attrs == null) return true;
        RelNode tableNode = ofTable(tableSym);
        if (tableNode == null) return true;

        try {
            RelMetadataQuery mq = createMetadataQuery();
            ImmutableBitSet.Builder builder = ImmutableBitSet.builder();
            for (ColumnRef ref : attrs) {
                int idx = resolveColumnIndex(ref, tableNode);
                if (idx < 0) return false;
                builder.set(idx);
            }
            ImmutableBitSet colBits = builder.build();

            Set<ImmutableBitSet> uniqueKeys = mq.getUniqueKeys(tableNode);
            if (uniqueKeys == null) return false;
            for (ImmutableBitSet ukey : uniqueKeys) {
                if (colBits.contains(ukey)) return true; // our columns contain a unique key
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * NOT_NULL(table, attrs): columns are not nullable.
     */
    @SuppressWarnings("unchecked")
    private boolean checkNotNull(Constraint c) {
        Symbol tableSym = c.symbols()[0];
        Symbol attrsSym = c.symbols()[1];
        List<ColumnRef> attrs = ofAttrs(attrsSym);
        if (attrs == null) return true;
        RelNode tableNode = ofTable(tableSym);
        if (tableNode == null) return true;

        for (ColumnRef ref : attrs) {
            int idx = resolveColumnIndex(ref, tableNode);
            if (idx < 0) return false;
            if (tableNode.getRowType().getFieldList().get(idx).getType().isNullable()) {
                return false;
            }
        }
        return true;
    }

    /**
     * REFERENCE(t0, a0, t1, a1): foreign key from t0.a0 to t1.a1.
     */
    @SuppressWarnings("unchecked")
    private boolean checkReference(Constraint c) {
        Symbol t0Sym = c.symbols()[0];
        Symbol a0Sym = c.symbols()[1];
        Symbol t1Sym = c.symbols()[2];
        Symbol a1Sym = c.symbols()[3];

        List<ColumnRef> a0 = ofAttrs(a0Sym);
        List<ColumnRef> a1 = ofAttrs(a1Sym);
        if (a0 == null || a1 == null) return true;

        // Check that both attr sets refer to equivalent columns
        return a0.equals(a1);
    }

    private int resolveColumnIndex(ColumnRef ref, RelNode node) {
        return ColumnRefResolver.resolveIndex(ref, node);
    }

    /**
     * Creates a fresh RelMetadataQuery with all default handlers properly initialized.
     * Mirrors {@code ConstraintUtils.createMetadataQuery()} — duplicated here
     * so that Model has no dependency on ConstraintUtils (which will be deleted
     * in a later task).
     */
    static RelMetadataQuery createMetadataQuery() {
        return new RelMetadataQuery(METADATA_PROVIDER);
    }
}
