package org.autorewriter.rewriter.rule.instantiation;

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
public class Instantiation {

    private final Model model;
    private final Constraints constraints;
    private final ColumnRefRegistry registry;

    private Instantiation(Model model, Constraints constraints) {
        this.model = model;
        this.constraints = constraints;
        this.registry = new ColumnRefRegistry();
    }

    public static RelNode instantiate(RelNode targetTemplate, Model model, Constraints constraints) {
        RelNode result = new Instantiation(model, constraints).instantiateNode(targetTemplate);
        // Normalize join trees to left-deep form (WeTune's normalizePlan → normalizeJoin)
        result = new NormalizeJoin().normalize(result);
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
                    projects.add(rexBuilder.makeInputRef(
                            child.getRowType().getFieldList().get(0).getType(), 0));
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
            }
            return result;
        }

        // Standard single-predicate filter instantiation
        RexNode condition = instantiateRexNode(templateCond, child);
        if (condition == null) condition = templateCond;

        RelNode result = LogicalFilter.create(child, condition);

        // Re-apply unmatched filters scoped to this filter's predicate symbol (virtualExpr).
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
        RexNode condition = instantiateRexNode(template.getCondition(), child);
        if (condition == null) condition = template.getCondition();

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
     * @return the result with virtualExprs applied, or unchanged if none exist
     */
    @SuppressWarnings("unchecked")
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
            if (reboundCond != null) {
                result = LogicalFilter.create(result, reboundCond);
            }
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
        int tLeftFieldCount = template.getLeft().getRowType().getFieldCount();

        // Extract join key pairs using FIELD NAMES (not index ranges).
        // Template tables have 10 columns each (a0-a9), so both key indices
        // may fall within the left table's range. We use field names from
        // the template's combined row type to identify the symbols.
        // Use left/right child field names directly (not disambiguated combined row type)
        List<String> tLeftFields = template.getLeft().getRowType().getFieldNames();
        List<String> tRightFields = template.getRight().getRowType().getFieldNames();
        List<RexInputRef[]> refPairs = extractRefPairs(templateCond);

        if (refPairs.isEmpty()) {
            RexNode inst = instantiateRexNode(templateCond, null);
            return inst != null ? inst : templateCond;
        }

        RexBuilder rexBuilder = left.getCluster().getRexBuilder();
        List<RexNode> equalities = new ArrayList<>();

        for (RexInputRef[] pair : refPairs) {
            int idx0 = pair[0].getIndex();
            int idx1 = pair[1].getIndex();

            // Resolve field names from the correct child
            String leftName = null, rightName = null;
            if (idx0 < tLeftFieldCount && idx1 >= tLeftFieldCount) {
                // idx0 from left, idx1 from right
                leftName = idx0 < tLeftFields.size() ? tLeftFields.get(idx0) : null;
                rightName = (idx1 - tLeftFieldCount) < tRightFields.size()
                        ? tRightFields.get(idx1 - tLeftFieldCount) : null;
            } else if (idx1 < tLeftFieldCount && idx0 >= tLeftFieldCount) {
                // idx1 from left, idx0 from right
                leftName = idx1 < tLeftFields.size() ? tLeftFields.get(idx1) : null;
                rightName = (idx0 - tLeftFieldCount) < tRightFields.size()
                        ? tRightFields.get(idx0 - tLeftFieldCount) : null;
            } else if (idx0 < tLeftFieldCount && idx1 < tLeftFieldCount) {
                // Both on left side — use left child's field names
                leftName = idx0 < tLeftFields.size() ? tLeftFields.get(idx0) : null;
                rightName = idx1 < tLeftFields.size() ? tLeftFields.get(idx1) : null;
            } else {
                // Both on right side — use right child's field names
                int rIdx0 = idx0 - tLeftFieldCount;
                int rIdx1 = idx1 - tLeftFieldCount;
                leftName = rIdx0 < tRightFields.size() ? tRightFields.get(rIdx0) : null;
                rightName = rIdx1 < tRightFields.size() ? tRightFields.get(rIdx1) : null;
            }

            int newLeftIdx = -1;
            int newRightIdx = -1;

            if (leftName != null && SymbolKind.isSymbolName(leftName)) {
                List<ColumnRef> refs = interpretAttrs(Symbol.of(leftName));
                if (refs == null) {
                    Symbol sym = Symbol.of(leftName);
                    Symbol src = constraints.instantiationOf(sym);
                    refs = model.ofAttrs(src != null ? src : sym);
                }
                if (refs != null && !refs.isEmpty()) {
                    newLeftIdx = registry.resolveIndex(refs.get(0), left);
                }
            }
            if (rightName != null && SymbolKind.isSymbolName(rightName)) {
                List<ColumnRef> refs = interpretAttrs(Symbol.of(rightName));
                if (refs == null) {
                    Symbol sym = Symbol.of(rightName);
                    Symbol src = constraints.instantiationOf(sym);
                    refs = model.ofAttrs(src != null ? src : sym);
                }
                if (refs != null && !refs.isEmpty()) {
                    newRightIdx = registry.resolveIndex(refs.get(0), right);
                }
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

        if (equalities.isEmpty()) return rexBuilder.makeLiteral(true);
        if (equalities.size() == 1) return equalities.get(0);
        return rexBuilder.makeCall(SqlStdOperatorTable.AND, equalities);
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
     */
    private RexNode rebindPredicateRefs(RexNode expr, RelNode sourceCtx, RelNode targetCtx) {
        if (expr instanceof RexInputRef) {
            int oldIdx = ((RexInputRef) expr).getIndex();
            // Use registry for consistent resolution
            List<ColumnRef> sourceCols = registry.outputColumnsOf(sourceCtx);
            if (oldIdx < sourceCols.size()) {
                ColumnRef ref = sourceCols.get(oldIdx);
                int newIdx = registry.resolveIndex(ref, targetCtx);
                if (newIdx >= 0) {
                    RexBuilder rexBuilder = targetCtx.getCluster().getRexBuilder();
                    RelDataType newType = targetCtx.getRowType().getFieldList().get(newIdx).getType();
                    return rexBuilder.makeInputRef(newType, newIdx);
                }
            }
            return expr;
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
}
