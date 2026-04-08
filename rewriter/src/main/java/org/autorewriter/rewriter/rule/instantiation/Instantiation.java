package org.autorewriter.rewriter.rule.instantiation;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.*;
import org.apache.calcite.rel.type.*;
import org.apache.calcite.rex.*;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.util.ImmutableBitSet;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.LogicalInSubFilter;
import org.autorewriter.rewriter.rule.constraint.Constraints;
import org.autorewriter.rewriter.rule.match.Match;
import org.autorewriter.rewriter.rule.model.Model;
import org.autorewriter.rewriter.rule.symbol.*;
import org.autorewriter.rewriter.rule.util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Constructs a target {@link RelNode} tree from a target template, a populated
 * {@link Model} (bound during matching), and {@link Constraints}.
 *
 * <p>Aligned with WeTune's Instantiation mechanism:
 * <ul>
 *   <li>{@link ColumnRefRegistry} tracks output column identities per node (like WeTune's ValuesRegistry)</li>
 *   <li>{@link #interpretAttrs} handles table swapping via positional mapping (like WeTune's interpretAttrs)</li>
 *   <li>{@link ColumnRefRegistry#resolveIndex} resolves column identity to position (like WeTune's rebindRefs)</li>
 * </ul>
 */
@Slf4j
public class Instantiation {

    private final Model model;
    private final Constraints constraints;
    private final ColumnRefRegistry registry;

    private Instantiation(Model model, Constraints constraints) {
        this.model = model;
        this.constraints = constraints;
        this.registry = new ColumnRefRegistry();
    }

    /**
     * Instantiate a target template into a concrete RelNode tree.
     *
     * @return the instantiated RelNode, or {@code null} if instantiation fails
     *         (e.g., foreign column references after table elimination)
     */
    public static RelNode instantiate(RelNode targetTemplate, Model model, Constraints constraints, RelOptCluster targetCluster) {
        RelNode result = new Instantiation(model, constraints).instantiateNode(targetTemplate);
        if (result == null) {
            return null;
        }

        result = new NormalizeJoin().normalize(result);
        result = rebuildFreshTree(result, targetCluster);
        return result;
    }

    private RelNode instantiateNode(RelNode template) {
        template = Match.unwrapHepVertex(template);

        if (template instanceof LogicalTableScan) {
            return instantiateInput((LogicalTableScan) template);
        }
        if (template instanceof LogicalProject) {
            return instantiateProject((LogicalProject) template);
        }
        if (template instanceof LogicalFilter) {
            return instantiateFilter((LogicalFilter) template);
        }
        if (template instanceof LogicalJoin) {
            return instantiateJoin((LogicalJoin) template);
        }
        if (template instanceof LogicalAggregate) {
            return instantiateAggregate((LogicalAggregate) template);
        }
        if (template instanceof LogicalInSubFilter) {
            return instantiateInSubFilterNode((LogicalInSubFilter) template);
        }
        return template;
    }

    // ── INPUT ──────────────────────────────────────────────────────────────

    private RelNode instantiateInput(LogicalTableScan template) {
        String tableName = getTableName(template);
        if (SymbolKind.isSymbolName(tableName) && tableName.charAt(0) == 't') {
            Symbol targetSym = Symbol.of(tableName);
            Symbol sourceSym = constraints.instantiationOf(targetSym);
            if (sourceSym != null) {
                RelNode bound = model.ofTable(sourceSym);
                if (bound != null) {
                    return bound;
                }
            }
            RelNode direct = model.ofTable(targetSym);
            if (direct != null) {
                return direct;
            }
        }
        return template;
    }

    // ── PROJ ───────────────────────────────────────────────────────────────

    private RelNode instantiateProject(LogicalProject template) {
        RelNode child = instantiateNode(template.getInput());
        if (child == null) return null;

        // Find attrs and schema placeholders
        List<String> templateFields = template.getRowType().getFieldNames();
        String attrsPlaceholder = null;
        String schemaPlaceholder = null;
        for (String field : templateFields) {
            if (SymbolKind.isSymbolName(field)) {
                if (field.charAt(0) == 'a') attrsPlaceholder = field;
                else if (field.charAt(0) == 's') schemaPlaceholder = field;
            }
        }

        // Use interpretAttrs for correct table-swap-aware resolution
        List<ColumnRef> columnRefs = null;
        if (attrsPlaceholder != null) {
            columnRefs = interpretAttrs(Symbol.of(attrsPlaceholder));
        }

        RelDataType schema = null;
        if (schemaPlaceholder != null) {
            Symbol targetSchemaSym = Symbol.of(schemaPlaceholder);
            Symbol sourceSchemaSym = constraints.instantiationOf(targetSchemaSym);
            schema = sourceSchemaSym != null ? model.ofSchema(sourceSchemaSym) : model.ofSchema(targetSchemaSym);
        }

        if (columnRefs == null || columnRefs.isEmpty()) {
            return child;
        }

        // Build projections using registry-based resolution
        RexBuilder rexBuilder = child.getCluster().getRexBuilder();
        List<RexNode> projects = new ArrayList<>();
        List<String> fieldNames = new ArrayList<>();

        for (ColumnRef ref : columnRefs) {
            int idx = registry.resolveIndex(ref, child);
            if (idx >= 0) {
                RelDataType fieldType = child.getRowType().getFieldList().get(idx).getType();
                projects.add(rexBuilder.makeInputRef(fieldType, idx));
            } else {
                // Fallback: try name matching on child's field names
                int nameIdx = findFieldByName(child, ref.getColumnName());
                if (nameIdx >= 0) {
                    RelDataType fieldType = child.getRowType().getFieldList().get(nameIdx).getType();
                    projects.add(rexBuilder.makeInputRef(fieldType, nameIdx));
                } else {
                    // Column not found — foreign value (WeTune FAILURE_FOREIGN_VALUE)
                    log.info("instantiateProject: column {} not resolvable in child", ref);
                    return null;
                }
            }
        }

        // Build field names
        if (schema != null) {
            fieldNames = new ArrayList<>(schema.getFieldNames());
        } else {
            for (ColumnRef ref : columnRefs) {
                fieldNames.add(ref.getColumnName());
            }
        }
        while (fieldNames.size() < projects.size()) fieldNames.add("col" + fieldNames.size());
        if (fieldNames.size() > projects.size()) fieldNames = new ArrayList<>(fieldNames.subList(0, projects.size()));

        return LogicalProject.create(child, Collections.emptyList(), projects, fieldNames);
    }

    // ── FILTER ─────────────────────────────────────────────────────────────

    private RelNode instantiateFilter(LogicalFilter template) {
        if (hasRexSubQuery(template.getCondition())) {
            return instantiateInSubFilter(template);
        }

        RelNode child = instantiateNode(template.getInput());
        if (child == null) return null;
        RexNode templateCond = template.getCondition();

        // ShardingSphere parser may merge nested Filter<p3>(Filter<p2>(...)) into a single
        // Filter with AND(p3, p2) compound condition. Detect this case and split into
        // separate LogicalFilter nodes so downstream rules (like 233) can match
        // Filter(Filter(Input)) patterns for filter merging/elimination.
        List<RexNode> predParts = splitAndPredicatePlaceholders(templateCond);
        if (predParts != null) {
            // Build filters bottom-up: last predicate first (innermost)
            RelNode result = child;
            for (int i = predParts.size() - 1; i >= 0; i--) {
                RexNode partCondition = instantiateRexNode(predParts.get(i), result);
                if (partCondition == null) partCondition = predParts.get(i);
                result = LogicalFilter.create(result, partCondition);
                result = applyVirtualExprs(predParts.get(i), result);
                if (result == null) return null;  // foreign value in virtualExpr
            }
            return result;
        }

        // Standard single-predicate filter instantiation
        RexNode condition = instantiateRexNode(templateCond, child);
        if (condition == null) {
            return null;
        }

        condition = fixRexTypes(condition, child);
        RelNode result = LogicalFilter.create(child, condition);

        // Re-apply unmatched filters scoped to this filter's predicate symbol (virtualExpr).
        // Pass the TEMPLATE condition (not the instantiated one) because extractPredSymbol
        // needs the placeholder operator name (p\d+) to find the virtualExpr key.
        result = applyVirtualExprs(templateCond, result);

        return result;
    }

    /**
     * If a template filter condition is {@code AND(p1(...), p2(...), ...)} where each
     * operand is a predicate placeholder, return the individual placeholder RexNodes.
     * This handles ShardingSphere parser merging nested {@code Filter<p3>(Filter<p2>(...))}
     * into a single {@code Filter(AND(p3, p2))}.
     *
     * @return list of individual predicate placeholder RexNodes, or null if not applicable
     */
    private static List<RexNode> splitAndPredicatePlaceholders(RexNode condition) {
        if (!(condition instanceof RexCall)) return null;
        RexCall call = (RexCall) condition;
        if (!"AND".equals(call.getOperator().getName())) return null;

        List<RexNode> parts = new ArrayList<>();
        for (RexNode operand : call.getOperands()) {
            if (!(operand instanceof RexCall)) return null;
            String opName = ((RexCall) operand).getOperator().getName();
            if (!SymbolKind.isSymbolName(opName) || opName.charAt(0) != 'p') return null;
            parts.add(operand);
        }
        return parts.isEmpty() ? null : parts;
    }

    private RelNode instantiateInSubFilter(LogicalFilter template) {
        RelNode child = instantiateNode(template.getInput());
        if (child == null) return null;
        RexNode condition = instantiateRexNode(template.getCondition(), child);
        if (condition == null) return null;  // subquery or predicate has foreign value

        // Produce LogicalInSubFilter to keep plan consistent with InSubFilterExpander
        RexSubQuery inSub = findInSubQuery(condition);
        if (inSub != null) {
            RexNode lhsRef = inSub.getOperands().get(0);

            // Resolve lhsRef using interpretAttrs: the template's $N maps to aN
            // in the template table, which through constraints maps to the actual column.
            // Without this, $7 (a7 in template) stays as $7 (closed_account) instead
            // of being resolved to $0 (people.id) via AttrsEq(a7, a3).
            if (lhsRef instanceof RexInputRef) {
                int templateIdx = ((RexInputRef) lhsRef).getIndex();
                RelNode templateChild = Match.unwrapHepVertex(template.getInput());
                List<String> templateFields = templateChild.getRowType().getFieldNames();
                if (templateIdx < templateFields.size()) {
                    String symName = templateFields.get(templateIdx);
                    if (SymbolKind.isSymbolName(symName) && symName.charAt(0) == 'a') {
                        List<ColumnRef> refs = interpretAttrs(Symbol.of(symName));
                        if (refs != null && !refs.isEmpty()) {
                            int newIdx = registry.resolveIndex(refs.get(0), child);
                            if (newIdx >= 0) {
                                RexBuilder rb = child.getCluster().getRexBuilder();
                                lhsRef = rb.makeInputRef(
                                        child.getRowType().getFieldList().get(newIdx).getType(), newIdx);
                            }
                        }
                    }
                }
            }

            LogicalInSubFilter inSubFilter = LogicalInSubFilter.create(child, inSub.rel, lhsRef);
            List<RexNode> remaining = removeSubQuery(condition, inSub);
            if (!remaining.isEmpty()) {
                RexNode remainingCond = RexUtil.composeConjunction(
                        child.getCluster().getRexBuilder(), remaining);
                return LogicalFilter.create(inSubFilter, remainingCond);
            }
            return inSubFilter;
        }
        return LogicalFilter.create(child, condition);
    }

    /**
     * Instantiate a target template that is already a {@link LogicalInSubFilter}
     */
    private RelNode instantiateInSubFilterNode(LogicalInSubFilter template) {
        // 1. Recursively instantiate both children
        RelNode left = instantiateNode(template.getLeft());
        RelNode right = instantiateNode(template.getRight());
        if (left == null || right == null) return null;

        // 2. Resolve lhsRef: map template's $N → attrs symbol → actual column index
        RexNode lhsRef = template.getLhsRef();
        if (lhsRef instanceof RexInputRef) {
            int templateIdx = ((RexInputRef) lhsRef).getIndex();
            RelNode templateLeft = Match.unwrapHepVertex(template.getLeft());
            List<String> templateFields = templateLeft.getRowType().getFieldNames();
            if (templateIdx < templateFields.size()) {
                String symName = templateFields.get(templateIdx);
                if (SymbolKind.isSymbolName(symName) && symName.charAt(0) == 'a') {
                    List<ColumnRef> refs = interpretAttrs(Symbol.of(symName));
                    if (refs != null && !refs.isEmpty()) {
                        int newIdx = registry.resolveIndex(refs.get(0), left);
                        if (newIdx >= 0) {
                            RexBuilder rb = left.getCluster().getRexBuilder();
                            lhsRef = rb.makeInputRef(
                                    left.getRowType().getFieldList().get(newIdx).getType(), newIdx);
                        }
                    }
                }
            }
        }

        return LogicalInSubFilter.create(left, right, lhsRef);
    }

    /**
     * Apply virtual expressions (unmatched filters) scoped to the predicate symbol
     * in the given template condition.
     *
     * <p>During matching, if a source filter chain had more filters than the template,
     * the extras were stored under {@code "virtualExpr_<predSymbol>"} in the Model
     * (e.g., {@code "virtualExpr_p0"}). Here we:
     * <ol>
     *   <li>Extract the target predicate symbol from the template condition (e.g., {@code p1})</li>
     *   <li>Map it to the source predicate via constraints (e.g., {@code p1 → p0})</li>
     *   <li>Look up {@code "virtualExpr_p0"} in the Model</li>
     *   <li>Rebind column indices and wrap with additional {@link LogicalFilter} nodes</li>
     * </ol>
     *
     * <p>This is WeTune's virtualExpr mechanism: per-predicate scoping ensures multiple
     * filter chains in the same rule don't interfere with each other.
     *
     * @param templateCondition the target template filter's condition (contains pred symbol)
     * @param result            the current instantiated node to wrap with extra filters
     * @return the result with virtualExprs applied, or {@code null} if any condition
     *         references a foreign column that cannot be resolved in the target
     */
    private RelNode applyVirtualExprs(RexNode templateCondition, RelNode result) {
        // 1. Extract target predicate symbol from template condition
        String targetPredName = extractPredSymbol(templateCondition);
        if (targetPredName == null) return result;

        // 2. Map to source predicate via constraints (e.g., PredicateEq(p1, p0) → p0)
        Symbol targetSym = Symbol.of(targetPredName);
        Symbol sourceSym = constraints.instantiationOf(targetSym);
        String sourcePredName = (sourceSym != null ? sourceSym : targetSym).name();

        // 3. Look up virtualExpr stored under source predicate key
        String key = "virtualExpr_" + sourcePredName;
        Object[] entry = (Object[]) model.ofExtra(key);
        if (entry == null) return result;

        // Consume once (clear after use)
        model.putExtra(key, null);

        List<RexNode> conditions = (List<RexNode>) entry[0];
        RelNode sourceContext = (RelNode) entry[1];
        if (sourceContext == null || conditions == null || conditions.isEmpty()) return result;

        // 4. Rebind each condition and wrap with LogicalFilter
        for (RexNode cond : conditions) {
            RexNode reboundCond = rebindPredicateRefs(cond, sourceContext, result);
            if (reboundCond == null) {
                log.info("virtualExpr rebind failed: foreign column in condition {}", cond);
                return null;
            }
            result = LogicalFilter.create(result, reboundCond);
        }
        return result;
    }

    /**
     * Extract predicate symbol name ({@code p\d+}) from a template condition.
     * Template filter conditions from the rule DSL are {@link RexCall} nodes whose
     * operator name is a predicate placeholder.
     */
    private static String extractPredSymbol(RexNode condition) {
        if (condition instanceof RexCall) {
            String opName = ((RexCall) condition).getOperator().getName();
            if (SymbolKind.isSymbolName(opName) && opName.charAt(0) == 'p') {
                return opName;
            }
        }
        return null;
    }

    // ── JOIN ───────────────────────────────────────────────────────────────

    private RelNode instantiateJoin(LogicalJoin template) {
        RelNode left = instantiateNode(template.getLeft());
        RelNode right = instantiateNode(template.getRight());
        if (left == null || right == null) return null;

        // Wrap non-trivial join inputs with identity projections (WeTune NormalizeProj).
        // This establishes clean column namespaces for each join side AND prevents
        // infinite rule re-matching in RBO (HepPlanner) by ensuring the output has
        // Proj layers that differ structurally from the input.
        // In CBO (VolcanoPlanner), the MEMO may eliminate these identity projections.
        // Match.matchJoinChild uses transparent Proj matching as fallback to handle
        // this case, allowing rule chaining to work across MEMO-stripped Proj layers.
        left = wrapWithIdentityProjectIfNeeded(left);
        right = wrapWithIdentityProjectIfNeeded(right);

        // Rebuild join condition using interpretAttrs + registry
        RexNode condition = rebuildJoinCondition(template, left, right);
        if(condition == null) {
            return null;
        }

        return LogicalJoin.create(left, right, Collections.emptyList(), condition,
                Collections.emptySet(), template.getJoinType());
    }

    /**
     * Wraps a RelNode with an identity LogicalProject if it is not a simple
     * LogicalTableScan. Aligned with WeTune's {@code NormalizeProj.insertProjBefore()}
     * which wraps Filter/Join nodes that are direct children of a Join with a
     * qualifying Proj that projects all columns through.
     */
    private static RelNode wrapWithIdentityProjectIfNeeded(RelNode node) {
        if (node instanceof LogicalTableScan) {
            return node;
        }
        // Don't double-wrap: if input is already an identity Projection, skip
        if (isIdentityProjection(node)) {
            return node;
        }
        RexBuilder rexBuilder = node.getCluster().getRexBuilder();
        RelDataType rowType = node.getRowType();
        List<RexNode> projects = new ArrayList<>();
        List<String> fieldNames = new ArrayList<>();
        for (int i = 0; i < rowType.getFieldCount(); i++) {
            RelDataTypeField field = rowType.getFieldList().get(i);
            projects.add(rexBuilder.makeInputRef(field.getType(), i));
            fieldNames.add(field.getName());
        }
        return LogicalProject.create(node, Collections.emptyList(), projects, fieldNames);
    }

    /**
     * Check if a node is an identity projection (projects all input columns
     * in order with no transformation). Used to avoid double-wrapping.
     */
    private static boolean isIdentityProjection(RelNode node) {
        if (!(node instanceof LogicalProject)) return false;
        LogicalProject proj = (LogicalProject) node;
        if (proj.getProjects().size() != proj.getInput().getRowType().getFieldCount()) return false;
        for (int i = 0; i < proj.getProjects().size(); i++) {
            RexNode expr = proj.getProjects().get(i);
            if (!(expr instanceof RexInputRef) || ((RexInputRef) expr).getIndex() != i) return false;
        }
        return true;
    }

    private RexNode rebuildJoinCondition(LogicalJoin template, RelNode left, RelNode right) {
        RexNode templateCond = template.getCondition();
        List<RexInputRef[]> refPairs = extractRefPairs(templateCond);

        if (refPairs.isEmpty()) {
            RexNode inst = instantiateRexNode(templateCond, null);
            return inst != null ? inst : templateCond;
        }

        // Use the join's combined row type to get symbol names for each index.
        // This is correct regardless of whether an index falls on the left or
        // right child — the combined row type is the authoritative mapping from
        // positional index to symbol name in the template.
        List<String> combinedFields = template.getRowType().getFieldNames();

        RexBuilder rexBuilder = left.getCluster().getRexBuilder();
        List<RexNode> equalities = new ArrayList<>();

        for (RexInputRef[] pair : refPairs) {
            int idx0 = pair[0].getIndex();
            int idx1 = pair[1].getIndex();

            String name0 = idx0 < combinedFields.size() ? combinedFields.get(idx0) : null;
            String name1 = idx1 < combinedFields.size() ? combinedFields.get(idx1) : null;

            // Resolve each symbol to ColumnRefs via interpretAttrs
            List<ColumnRef> refs0 = resolveJoinKeyRefs(name0);
            List<ColumnRef> refs1 = resolveJoinKeyRefs(name1);

            if (refs0 == null || refs0.isEmpty() || refs1 == null || refs1.isEmpty()) {
                continue;
            }

            // Try to resolve each ref on both sides; the one that resolves
            // on left becomes the left key, the other becomes the right key.
            int leftIdx0 = registry.resolveIndex(refs0.get(0), left);
            int rightIdx0 = registry.resolveIndex(refs0.get(0), right);
            int leftIdx1 = registry.resolveIndex(refs1.get(0), left);
            int rightIdx1 = registry.resolveIndex(refs1.get(0), right);

            int newLeftIdx = -1;
            int newRightIdx = -1;

            if (leftIdx0 >= 0 && rightIdx1 >= 0) {
                // name0 resolves on left, name1 resolves on right
                newLeftIdx = leftIdx0;
                newRightIdx = rightIdx1;
            } else if (leftIdx1 >= 0 && rightIdx0 >= 0) {
                // name1 resolves on left, name0 resolves on right
                newLeftIdx = leftIdx1;
                newRightIdx = rightIdx0;
            } else if (leftIdx0 >= 0 && leftIdx1 >= 0) {
                // Both resolve on left (self-join scenario) — use combined row type
                // position to determine original left/right assignment
                newLeftIdx = leftIdx0;
                newRightIdx = leftIdx1;
            } else if (rightIdx0 >= 0 && rightIdx1 >= 0) {
                // Both resolve on right
                newLeftIdx = rightIdx0;
                newRightIdx = rightIdx1;
            }

            if (newLeftIdx >= 0 && newRightIdx >= 0) {
                RelDataType leftType = left.getRowType().getFieldList().get(newLeftIdx).getType();
                RelDataType rightType = right.getRowType().getFieldList().get(newRightIdx).getType();
                RexNode leftRef = rexBuilder.makeInputRef(leftType, newLeftIdx);
                RexNode rightRef = rexBuilder.makeInputRef(rightType,
                        left.getRowType().getFieldCount() + newRightIdx);
                equalities.add(rexBuilder.makeCall(SqlStdOperatorTable.EQUALS, leftRef, rightRef));
            }
        }

        if (equalities.isEmpty() && refPairs.isEmpty()) {
            return rexBuilder.makeLiteral(true);
        } else if (equalities.isEmpty()) {
            return null;
        }

        if (equalities.size() == 1) return equalities.get(0);
        return rexBuilder.makeCall(SqlStdOperatorTable.AND, equalities);
    }

    /**
     * Resolve a symbol name to ColumnRefs for join key rebuilding.
     * Tries interpretAttrs first, falls back to direct model lookup.
     */
    private List<ColumnRef> resolveJoinKeyRefs(String symbolName) {
        if (symbolName == null || !SymbolKind.isSymbolName(symbolName)) {
            return null;
        }
        List<ColumnRef> refs = interpretAttrs(Symbol.of(symbolName));
        if (refs == null) {
            Symbol sym = Symbol.of(symbolName);
            Symbol src = constraints.instantiationOf(sym);
            refs = model.ofAttrs(src != null ? src : sym);
        }
        return refs;
    }

    /**
     * Extract equi-join RexInputRef pairs from a condition, regardless of
     * which side they're on (unlike extractJoinKeyPairs which requires
     * left < threshold < right).
     */
    private static List<RexInputRef[]> extractRefPairs(RexNode condition) {
        List<RexInputRef[]> pairs = new ArrayList<>();
        extractRefPairsRecursive(condition, pairs);
        return pairs;
    }

    private static void extractRefPairsRecursive(RexNode condition, List<RexInputRef[]> pairs) {
        if (condition instanceof org.apache.calcite.rex.RexCall) {
            org.apache.calcite.rex.RexCall call = (org.apache.calcite.rex.RexCall) condition;
            String opName = call.getOperator().getName();
            if ("=".equals(opName) && call.getOperands().size() == 2) {
                RexNode l = call.getOperands().get(0);
                RexNode r = call.getOperands().get(1);
                if (l instanceof RexInputRef && r instanceof RexInputRef) {
                    pairs.add(new RexInputRef[]{(RexInputRef) l, (RexInputRef) r});
                }
            } else if ("AND".equals(opName)) {
                for (RexNode operand : call.getOperands()) {
                    extractRefPairsRecursive(operand, pairs);
                }
            }
        }
    }

    // ── AGGREGATE ──────────────────────────────────────────────────────────

    private RelNode instantiateAggregate(LogicalAggregate template) {
        RelNode child = instantiateNode(template.getInput());
        if (child == null) return null;

        List<String> templateFields = template.getRowType().getFieldNames();
        ImmutableBitSet groupSet = template.getGroupSet();

        for (String field : templateFields) {
            if (SymbolKind.isSymbolName(field) && field.charAt(0) == 'a') {
                List<ColumnRef> refs = interpretAttrs(Symbol.of(field));
                if (refs != null) {
                    ImmutableBitSet.Builder builder = ImmutableBitSet.builder();
                    for (ColumnRef ref : refs) {
                        int idx = registry.resolveIndex(ref, child);
                        if (idx >= 0) builder.set(idx);
                    }
                    groupSet = builder.build();
                }
                break;
            }
        }

        return LogicalAggregate.create(child, Collections.emptyList(), groupSet,
                null, template.getAggCallList());
    }

    // ── interpretAttrs (WeTune-aligned) ────────────────────────────────────

    /**
     * Resolve a target attrs symbol to concrete ColumnRefs, handling table swapping.
     * This is the equivalent of WeTune's {@code Instantiation.interpretAttrs()}.
     *
     * <p>When a rule swaps tables (e.g., target has {@code InnerJoin(t2=t1, t3=t0)}),
     * the attrs from the source side may need to be mapped positionally from the
     * nominal source table to the actual source table.
     */
    private List<ColumnRef> interpretAttrs(Symbol targetAttrs) {
        Symbol sourceAttrs = constraints.instantiationOf(targetAttrs);
        if (sourceAttrs == null) sourceAttrs = targetAttrs;

        List<ColumnRef> nominalRefs = model.ofAttrs(sourceAttrs);
        if (nominalRefs == null) return null;

        // Find the table that this target attrs belongs to (via AttrsSub constraint)
        Symbol actualSourceTable = constraints.sourceOf(targetAttrs);
        Symbol nominalSourceTable = constraints.sourceOf(sourceAttrs);

        if (actualSourceTable == null || nominalSourceTable == null) {
            return nominalRefs;  // no AttrsSub info, return as-is
        }

        // Resolve table symbols to their bound nodes
        Symbol actualTableInstantiated = constraints.instantiationOf(actualSourceTable);
        if (actualTableInstantiated == null) actualTableInstantiated = actualSourceTable;

        // Check if the actual and nominal source tables map to the same node
        RelNode actualNode = model.ofTable(actualTableInstantiated);
        RelNode nominalNode = model.ofTable(nominalSourceTable);

        if (actualNode == null || nominalNode == null || actualNode == nominalNode) {
            return nominalRefs;  // same table or can't resolve, no mapping needed
        }

        // Table swap detected: map columns positionally from nominal to actual
        List<ColumnRef> actualCols = registry.outputColumnsOf(actualNode);
        List<ColumnRef> nominalCols = registry.outputColumnsOf(nominalNode);

        List<ColumnRef> result = new ArrayList<>();
        for (ColumnRef nominal : nominalRefs) {
            int idx = nominalCols.indexOf(nominal);
            if (idx >= 0 && idx < actualCols.size()) {
                result.add(actualCols.get(idx));
            } else {
                result.add(nominal);  // fallback
            }
        }
        return result;
    }

    // ── RexNode instantiation ──────────────────────────────────────────────

    private RexNode instantiateRexNode(RexNode template, RelNode context) {
        if (template instanceof RexSubQuery) {
            RexSubQuery sub = (RexSubQuery) template;
            RelNode newRel = instantiateNode(sub.rel);
            if (newRel == null) return null;  // subquery child has foreign value
            List<RexNode> newOperands = new ArrayList<>();
            for (RexNode operand : sub.getOperands()) {
                RexNode inst = instantiateRexNode(operand, context);
                newOperands.add(inst != null ? inst : operand);
            }
            return sub.clone(newRel).clone(sub.getType(), newOperands);
        }

        if (template instanceof RexCall) {
            RexCall call = (RexCall) template;
            String opName = call.getOperator().getName();

            // Predicate placeholder
            if (SymbolKind.isSymbolName(opName) && opName.charAt(0) == 'p') {
                Symbol targetSym = Symbol.of(opName);
                Symbol sourceSym = constraints.instantiationOf(targetSym);
                RexNode bound = sourceSym != null ? model.ofPred(sourceSym) : model.ofPred(targetSym);
                if (bound != null) {
                    String predName = (sourceSym != null ? sourceSym : targetSym).name();
                    RelNode sourceContext = (RelNode) model.ofExtra(predName + "_context");
                    if (sourceContext != null && context != null) {
                        return rebindPredicateRefs(bound, sourceContext, context);
                    }
                    return bound;
                }
            }

            // Recursively instantiate operands
            List<RexNode> newOperands = new ArrayList<>();
            boolean changed = false;
            for (RexNode operand : call.getOperands()) {
                RexNode inst = instantiateRexNode(operand, context);
                if (inst != null && inst != operand) {
                    newOperands.add(inst);
                    changed = true;
                } else {
                    newOperands.add(operand);
                }
            }
            if (changed) return call.clone(call.getType(), newOperands);
            return call;
        }

        // RexInputRef: rebind type from context
        if (template instanceof RexInputRef && context != null) {
            RexInputRef ref = (RexInputRef) template;
            int idx = ref.getIndex();
            if (idx < context.getRowType().getFieldCount()) {
                RelDataType correctType = context.getRowType().getFieldList().get(idx).getType();
                if (!correctType.equals(ref.getType())) {
                    return context.getCluster().getRexBuilder().makeInputRef(correctType, idx);
                }
            }
        }

        return template;
    }

    /**
     * Rebind predicate RexInputRef indices from source context to target context
     * using the ColumnRefRegistry.
     *
     * <p>Returns {@code null} if any RexInputRef cannot be resolved in the target
     * context (WeTune's FAILURE_FOREIGN_VALUE equivalent). This happens when a
     * rewrite eliminates a table whose columns are still referenced by predicates.
     */
    private RexNode rebindPredicateRefs(RexNode expr, RelNode sourceCtx, RelNode targetCtx) {
        if (expr instanceof RexInputRef) {
            int oldIdx = ((RexInputRef) expr).getIndex();
            List<ColumnRef> sourceCols = registry.outputColumnsOf(sourceCtx);
            if (oldIdx < sourceCols.size()) {
                ColumnRef ref = sourceCols.get(oldIdx);
                int newIdx = registry.resolveIndex(ref, targetCtx);
                if (newIdx >= 0 && newIdx < targetCtx.getRowType().getFieldCount()) {
                    RexBuilder rexBuilder = targetCtx.getCluster().getRexBuilder();
                    RelDataType newType = targetCtx.getRowType().getFieldList().get(newIdx).getType();
                    return rexBuilder.makeInputRef(newType, newIdx);
                }
            }
            // Column not found in target — foreign value
            return null;
        }
        if (expr instanceof RexSubQuery) {
            return expr;  // subquery operands reference their own context
        }
        if (expr instanceof RexCall) {
            RexCall call = (RexCall) expr;
            List<RexNode> newOperands = new ArrayList<>();
            boolean changed = false;
            for (RexNode operand : call.getOperands()) {
                RexNode rebound = rebindPredicateRefs(operand, sourceCtx, targetCtx);
                if (rebound == null) return null;  // propagate failure
                newOperands.add(rebound);
                if (rebound != operand) changed = true;
            }
            return changed ? call.clone(call.getType(), newOperands) : call;
        }
        return expr;
    }

    // ── Utilities ──────────────────────────────────────────────────────────

    private static String getTableName(LogicalTableScan scan) {
        List<String> names = scan.getTable().getQualifiedName();
        return names.get(names.size() - 1);
    }

    /**
     * Fix RexInputRef types to match the input node's row type.
     * Recursively processes RexCall and RexSubQuery operands.
     */
    private static RexNode fixRexTypes(RexNode expr, RelNode input) {
        if (expr instanceof RexInputRef) {
            RexInputRef ref = (RexInputRef) expr;
            int idx = ref.getIndex();
            if (idx < input.getRowType().getFieldCount()) {
                return input.getCluster().getRexBuilder().makeInputRef(
                    input.getRowType().getFieldList().get(idx).getType(), idx);
            }
            return ref;
        }
        if (expr instanceof RexSubQuery) {
            RexSubQuery sub = (RexSubQuery) expr;
            List<RexNode> newOps = new ArrayList<>();
            for (RexNode op : sub.getOperands()) {
                newOps.add(fixRexTypes(op, input));
            }
            return sub.clone(sub.rel).clone(sub.getType(), newOps);
        }
        if (expr instanceof RexCall) {
            RexCall call = (RexCall) expr;
            List<RexNode> newOps = new ArrayList<>();
            for (RexNode op : call.getOperands()) {
                newOps.add(fixRexTypes(op, input));
            }
            return input.getCluster().getRexBuilder().makeCall(call.getOperator(), newOps);
        }
        return expr;
    }

    private static int findFieldByName(RelNode node, String name) {
        List<String> fields = node.getRowType().getFieldNames();
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).equalsIgnoreCase(name)) return i;
        }
        return -1;
    }

    private static boolean hasRexSubQuery(RexNode node) {
        if (node instanceof RexSubQuery) return true;
        if (node instanceof RexCall) {
            for (RexNode operand : ((RexCall) node).getOperands()) {
                if (hasRexSubQuery(operand)) return true;
            }
        }
        return false;
    }

    private static RexSubQuery findInSubQuery(RexNode condition) {
        if (condition instanceof RexSubQuery) {
            RexSubQuery sub = (RexSubQuery) condition;
            if (sub.getKind() == SqlKind.IN) return sub;
        }
        if (condition instanceof RexCall) {
            for (RexNode operand : ((RexCall) condition).getOperands()) {
                RexSubQuery found = findInSubQuery(operand);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static List<RexNode> removeSubQuery(RexNode condition, RexSubQuery target) {
        List<RexNode> conjunctions = RexUtil.flattenAnd(Collections.singletonList(condition));
        List<RexNode> remaining = new ArrayList<>();
        for (RexNode conj : conjunctions) {
            if (conj != target) remaining.add(conj);
        }
        return remaining;
    }

    private static List<int[]> extractJoinKeyPairs(RexNode condition, int leftFieldCount) {
        List<int[]> pairs = new ArrayList<>();
        extractJoinKeyPairsRecursive(condition, leftFieldCount, pairs);
        return pairs;
    }

    private static void extractJoinKeyPairsRecursive(RexNode condition, int leftFieldCount, List<int[]> pairs) {
        if (condition instanceof RexCall) {
            RexCall call = (RexCall) condition;
            String opName = call.getOperator().getName();
            if ("=".equals(opName) && call.getOperands().size() == 2) {
                RexNode left = call.getOperands().get(0);
                RexNode right = call.getOperands().get(1);
                if (left instanceof RexInputRef && right instanceof RexInputRef) {
                    int leftIdx = ((RexInputRef) left).getIndex();
                    int rightIdx = ((RexInputRef) right).getIndex();
                    if (leftIdx < leftFieldCount && rightIdx >= leftFieldCount) {
                        pairs.add(new int[]{leftIdx, rightIdx});
                    } else if (rightIdx < leftFieldCount && leftIdx >= leftFieldCount) {
                        pairs.add(new int[]{rightIdx, leftIdx});
                    }
                }
            } else if ("AND".equals(opName)) {
                for (RexNode operand : call.getOperands()) {
                    extractJoinKeyPairsRecursive(operand, leftFieldCount, pairs);
                }
            }
        }
    }

    /**
     * Rebuild the entire RelNode tree using {@code LogicalXxx.create()} to produce
     * fresh nodes without any VolcanoPlanner registration state (no {@code rel#} IDs).
     *
     * <p>During matching, query sub-trees are stored in the Model. These nodes are
     * already registered in the VolcanoPlanner (they have {@code rel#} IDs). When
     * Instantiation reuses them in a new tree, {@code call.transformTo()} tries to
     * register the new tree but finds child nodes that are already registered,
     * triggering "belongs to a different planner" errors.
     *
     * <p>This method creates a completely fresh copy of the tree where every node
     * is newly created via its static {@code create()} factory method, ensuring no
     * pre-existing planner registration.
     */
    static RelNode rebuildFreshTree(RelNode node, RelOptCluster targetCluster) {
        // Unwrap VolcanoPlanner's RelSubset and HepPlanner's HepRelVertex
        node = unwrapPlannerNode(node);

        // Recursively rebuild children first
        List<RelNode> freshInputs = new ArrayList<>();
        for (RelNode input : node.getInputs()) {
            freshInputs.add(rebuildFreshTree(input, targetCluster));
        }

        RexBuilder rexBuilder = targetCluster.getRexBuilder();

        if (node instanceof LogicalTableScan) {
            LogicalTableScan scan = (LogicalTableScan) node;
            return LogicalTableScan.create(targetCluster, scan.getTable(), scan.getHints());
        }
        if (node instanceof LogicalFilter) {
            LogicalFilter filter = (LogicalFilter) node;
            RelNode newInput = freshInputs.get(0);
            RexNode newCond = rebuildRex(filter.getCondition(), rexBuilder, targetCluster, newInput);
            return LogicalFilter.create(newInput, newCond);
        }
        if (node instanceof LogicalProject) {
            LogicalProject proj = (LogicalProject) node;
            RelNode newInput = freshInputs.get(0);
            List<RexNode> newProjects = new ArrayList<>();
            for (RexNode expr : proj.getProjects()) {
                newProjects.add(rebuildRex(expr, rexBuilder, targetCluster, newInput));
            }
            return LogicalProject.create(newInput, Collections.emptyList(),
                    newProjects, proj.getRowType().getFieldNames());
        }
        if (node instanceof LogicalJoin) {
            LogicalJoin join = (LogicalJoin) node;
            RelNode newLeft = freshInputs.get(0);
            RelNode newRight = freshInputs.get(1);
            RexNode newCond = rebuildRex(join.getCondition(), rexBuilder, targetCluster, newLeft, newRight);
            return LogicalJoin.create(newLeft, newRight, Collections.emptyList(),
                    newCond, Collections.emptySet(), join.getJoinType());
        }
        if (node instanceof LogicalAggregate) {
            LogicalAggregate agg = (LogicalAggregate) node;
            return LogicalAggregate.create(freshInputs.get(0), agg.getGroupSet(),
                    agg.getGroupSets(), agg.getAggCallList());
        }
        if (node instanceof LogicalSort) {
            LogicalSort sort = (LogicalSort) node;
            return LogicalSort.create(freshInputs.get(0),
                    sort.getCollation(), sort.offset, sort.fetch);
        }
        if (node instanceof LogicalInSubFilter) {
            LogicalInSubFilter inSub = (LogicalInSubFilter) node;
            RelNode newLeft = freshInputs.get(0);
            RelNode newRight = freshInputs.get(1);
            RexNode newLhsRef = rebuildRex(inSub.getLhsRef(), rexBuilder, targetCluster, newLeft);
            return LogicalInSubFilter.create(newLeft, newRight, newLhsRef);
        }

        // Fallback
        if (!freshInputs.isEmpty()) {
            return node.copy(node.getTraitSet(), freshInputs);
        }
        return node;
    }

    /**
     * Rebuild a RexNode using the given RexBuilder, resolving RexInputRef types
     * from the combined row type of the input nodes.
     *
     * @param expr       the expression to rebuild
     * @param rexBuilder the RexBuilder to use
     * @param targetCluster the target cluster for rebuilding subqueries
     * @param inputs     the input nodes whose row types determine RexInputRef types
     *                   (single node for Filter/Project, left+right for Join)
     */
    private static RexNode rebuildRex(RexNode expr, RexBuilder rexBuilder, RelOptCluster targetCluster, RelNode... inputs) {
        if (expr instanceof RexInputRef) {
            int idx = ((RexInputRef) expr).getIndex();
            int offset = 0;
            for (RelNode input : inputs) {
                int fieldCount = input.getRowType().getFieldCount();
                if (idx - offset < fieldCount) {
                    return rexBuilder.makeInputRef(
                            input.getRowType().getFieldList().get(idx - offset).getType(), idx);
                }
                offset += fieldCount;
            }
            return expr;
        }
        // RexSubQuery extends RexCall — must check before RexCall
        if (expr instanceof RexSubQuery) {
            RexSubQuery sub = (RexSubQuery) expr;
            List<RexNode> newOps = new ArrayList<>();
            for (RexNode op : sub.getOperands()) {
                newOps.add(rebuildRex(op, rexBuilder, targetCluster, inputs));
            }
            RelNode newRel = rebuildFreshTree(sub.rel, targetCluster);
            // First clone with new operands, then clone with new rel
            RexSubQuery withNewOps = (RexSubQuery) sub.clone(sub.getType(), newOps);
            return withNewOps.clone(newRel);
        }
        if (expr instanceof RexCall) {
            RexCall call = (RexCall) expr;
            List<RexNode> newOps = new ArrayList<>();
            for (RexNode op : call.getOperands()) {
                newOps.add(rebuildRex(op, rexBuilder, targetCluster, inputs));
            }
            return call.clone(call.getType(), newOps);
        }
        return expr;
    }

    /**
     * Unwrap planner wrapper nodes (RelSubset from VolcanoPlanner, HepRelVertex
     * from HepPlanner) to get the underlying logical node.
     */
    private static RelNode unwrapPlannerNode(RelNode node) {
        while (node instanceof org.apache.calcite.plan.hep.HepRelVertex) {
            node = ((org.apache.calcite.plan.hep.HepRelVertex) node).getCurrentRel();
        }
        if (node instanceof org.apache.calcite.plan.volcano.RelSubset) {
            RelNode original = ((org.apache.calcite.plan.volcano.RelSubset) node).getOriginal();
            if (original != null) {
                return unwrapPlannerNode(original);
            }
        }
        return node;
    }
}
