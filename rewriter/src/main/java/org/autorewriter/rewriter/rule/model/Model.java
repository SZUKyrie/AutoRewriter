package org.autorewriter.rewriter.rule.model;

import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelReferentialConstraint;
import org.apache.calcite.rel.logical.LogicalAggregate;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rel.logical.LogicalTableScan;
import org.apache.calcite.rel.metadata.DefaultRelMetadataProvider;
import org.apache.calcite.rel.metadata.JaninoRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.calcite.util.mapping.IntPair;
import org.autorewriter.rewriter.rule.constraint.Constraint;
import org.autorewriter.rewriter.rule.constraint.Constraints;
import org.autorewriter.rewriter.rule.symbol.Symbol;
import org.autorewriter.rewriter.rule.symbol.SymbolKind;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.autorewriter.rewriter.rule.util.ColumnRefResolver;

import java.util.ArrayList;
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
        if (a0.size() != a1.size()) return false;
        // Compare at schema level (strip self-join disambiguation suffix),
        // aligned with WeTune's isAttrsEq which compares Column objects.
        for (int i = 0; i < a0.size(); i++) {
            if (!a0.get(i).schemaEquals(a1.get(i))) return false;
        }
        return true;
    }

    /**
     * Check predicate compatibility for PredicateEq constraint.
     *
     * <p>Aligned with WeTune's approach: column references are replaced with placeholders
     * before comparison, so that structurally identical predicates with different column
     * indices (e.g., {@code =($0, 488)} vs {@code =($5, 488)}) are considered equal.
     * WeTune uses {@code Expression.template()} which replaces all column refs with
     * {@code #.#}; we achieve the same effect by stripping RexInputRef indices.
     */
    private boolean checkPredCompatible(Object v0, Object v1) {
        if (!(v0 instanceof RexNode) || !(v1 instanceof RexNode)) return false;
        return normalizePredicate((RexNode) v0).equals(normalizePredicate((RexNode) v1));
    }

    /**
     * Normalize a predicate RexNode for equality comparison by replacing all
     * {@link org.apache.calcite.rex.RexInputRef} indices with a placeholder {@code #}.
     * This mirrors WeTune's {@code Expression.template()} which uses {@code #.#}.
     */
    private static String normalizePredicate(RexNode node) {
        return node.toString().replaceAll("\\$\\d+", "\\#");
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
     * REFERENCE(t0, a0, t1, a1): foreign key from t0.a0 referencing t1.a1.
     *
     * <p>Aligned with WeTune's {@code Model.checkReference()} (lines 216-256):
     * <ol>
     *   <li>Resolve a0/a1 to column refs; if a0 == a1 (reflexive), skip FK lookup
     *       but still run the path-filter check</li>
     *   <li>Otherwise look up FK constraints from the referring table's schema metadata</li>
     *   <li>Check that no Filter/Having exists on the path from the referred
     *       column's origin table up to the t1 subtree root (filters invalidate FK guarantees)</li>
     * </ol>
     */
    @SuppressWarnings("unchecked")
    private boolean checkReference(Constraint c) {
        Symbol t0Sym = c.symbols()[0]; // referring table
        Symbol a0Sym = c.symbols()[1]; // referring attrs
        Symbol t1Sym = c.symbols()[2]; // referred table
        Symbol a1Sym = c.symbols()[3]; // referred attrs

        List<ColumnRef> a0 = ofAttrs(a0Sym);
        if (a0 == null) return true; // not assigned yet
        List<ColumnRef> a1 = ofAttrs(a1Sym);
        if (a1 == null) return true; // not assigned yet

        // Step 1: FK lookup — skip only when a0 == a1 (reflexive, same columns)
        // Aligned with WeTune lines 235-236:
        //   if (!referringColumns.equals(referredCols)
        //       && linearFind(fks, ...) == null) return false;
        if (!a0.equals(a1)) {
            // Find the referring table's LogicalTableScan to access FK metadata
            RelNode t0Node = ofTable(t0Sym);
            if (t0Node == null) return true; // not assigned yet
            LogicalTableScan t0Scan = findTableScan(t0Node);
            if (t0Scan == null) return false; // can't resolve table

            RelOptTable t0Table = t0Scan.getTable();
            List<RelReferentialConstraint> fks = t0Table.getReferentialConstraints();
            if (fks == null || fks.isEmpty()) return false;

            // Resolve referring column names from a0
            List<String> referringColNames = new ArrayList<>(a0.size());
            for (ColumnRef ref : a0) referringColNames.add(ref.getColumnName().toLowerCase());

            // Resolve referred column names from a1
            List<String> referredColNames = new ArrayList<>(a1.size());
            for (ColumnRef ref : a1) referredColNames.add(ref.getColumnName().toLowerCase());

            // Resolve referred table name from a1 (all ColumnRefs in a1 should share the same table)
            String referredTableName = a1.get(0).getTableName().toLowerCase();

            // Check if any FK matches
            List<String> t0ColNames = t0Table.getRowType().getFieldNames();
            boolean fkFound = false;
            for (RelReferentialConstraint fk : fks) {
                // Check target table matches
                List<String> targetQName = fk.getTargetQualifiedName();
                String fkTargetTable = targetQName.get(targetQName.size() - 1).toLowerCase();
                if (!referredTableName.contains(fkTargetTable) && !fkTargetTable.contains(referredTableName)) {
                    // Try matching just the table name part
                    String referredSimpleName = referredTableName;
                    int dotIdx = referredSimpleName.lastIndexOf('.');
                    if (dotIdx >= 0) referredSimpleName = referredSimpleName.substring(dotIdx + 1);
                    if (!fkTargetTable.equals(referredSimpleName)) continue;
                }

                // Check column pairs match
                List<IntPair> pairs = fk.getColumnPairs();
                if (pairs.size() != referringColNames.size()) continue;

                // Resolve FK source column names and check they match a0
                boolean colsMatch = true;
                List<String> fkReferredColNames = new ArrayList<>(pairs.size());
                for (IntPair pair : pairs) {
                    String srcCol = t0ColNames.get(pair.source).toLowerCase();
                    if (!referringColNames.contains(srcCol)) { colsMatch = false; break; }
                    fkReferredColNames.add(String.valueOf(pair.target));
                }
                if (!colsMatch) continue;

                // Resolve target column names: find the referred table's LogicalTableScan
                RelNode t1Node = ofTable(t1Sym);
                if (t1Node == null) return true;
                LogicalTableScan t1Scan = findTableScan(t1Node);
                if (t1Scan != null) {
                    List<String> t1ColNames = t1Scan.getTable().getRowType().getFieldNames();
                    boolean targetColsMatch = true;
                    for (int i = 0; i < pairs.size(); i++) {
                        int targetIdx = pairs.get(i).target;
                        if (targetIdx >= t1ColNames.size()) { targetColsMatch = false; break; }
                        String targetCol = t1ColNames.get(targetIdx).toLowerCase();
                        if (!referredColNames.contains(targetCol)) { targetColsMatch = false; break; }
                    }
                    if (!targetColsMatch) continue;
                }

                fkFound = true;
                break;
            }
            if (!fkFound) return false;
        }

        // Step 2: Path filter check (WeTune lines 241-253)
        // Always executed, even when a0 == a1 (reflexive).
        // If the referred side (t1) has filters between the origin table and the
        // surface node, the FK guarantee is invalidated.
        RelNode t1Node = ofTable(t1Sym);
        if (t1Node == null) return true;
        if (hasFilterOnPath(t1Node)) return false;

        return true;
    }

    /**
     * Find the first LogicalTableScan in a RelNode subtree (depth-first).
     */
    private static LogicalTableScan findTableScan(RelNode node) {
        node = unwrap(node);
        if (node instanceof LogicalTableScan) return (LogicalTableScan) node;
        for (RelNode input : node.getInputs()) {
            LogicalTableScan scan = findTableScan(input);
            if (scan != null) return scan;
        }
        return null;
    }

    /**
     * Check if there is a Filter or Aggregate-with-Having on the path from root
     * down to any LogicalTableScan. Aligned with WeTune's path filter check
     * (Model.java lines 241-253) which invalidates FK when the referred side is filtered.
     */
    private static boolean hasFilterOnPath(RelNode node) {
        node = unwrap(node);
        if (node instanceof LogicalTableScan) return false; // reached leaf, no filter
        if (node instanceof LogicalFilter) return true;
        if (node instanceof LogicalAggregate) {
            // Check if aggregate has a having clause (non-empty agg calls can filter)
            // In Calcite, having is represented as a Filter on top of Aggregate,
            // so we just check for Filter nodes in the tree
        }
        for (RelNode input : node.getInputs()) {
            if (hasFilterOnPath(input)) return true;
        }
        return false;
    }

    /**
     * Unwrap HepRelVertex to get the underlying RelNode.
     */
    private static RelNode unwrap(RelNode node) {
        while (node instanceof HepRelVertex) {
            node = ((HepRelVertex) node).getCurrentRel();
        }
        return node;
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
