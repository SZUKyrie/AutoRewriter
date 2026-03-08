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
                if (bound != null) return bound;
            }
            RelNode direct = model.ofTable(targetSym);
            if (direct != null) return direct;
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
        RexNode condition = instantiateRexNode(template.getCondition(), child);
        if (condition == null) condition = template.getCondition();

        return LogicalFilter.create(child, condition);
    }

    private RelNode instantiateInSubFilter(LogicalFilter template) {
        RelNode child = instantiateNode(template.getInput());
        RexNode condition = instantiateRexNode(template.getCondition(), child);
        if (condition == null) condition = template.getCondition();

        // Produce LogicalInSubFilter to keep plan consistent with InSubFilterExpander
        RexSubQuery inSub = findInSubQuery(condition);
        if (inSub != null) {
            RexNode lhsRef = inSub.getOperands().get(0);
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

    // ── JOIN ───────────────────────────────────────────────────────────────

    private RelNode instantiateJoin(LogicalJoin template) {
        RelNode left = instantiateNode(template.getLeft());
        RelNode right = instantiateNode(template.getRight());

        // Wrap non-trivial join inputs with identity projections (WeTune NormalizeProj).
        // This establishes clean column namespaces for each join side, which:
        // 1. Enables correct column disambiguation in self-joins (same-name columns)
        // 2. Allows RelToSqlConverter to generate proper derived table aliases
        // 3. Creates a stable column identity layer for ColumnRefRegistry resolution
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
            return node; // TableScan already has a clean namespace
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

    private RexNode rebuildJoinCondition(LogicalJoin template, RelNode left, RelNode right) {
        RexNode templateCond = template.getCondition();
        int tLeftFieldCount = template.getLeft().getRowType().getFieldCount();
        List<int[]> templatePairs = extractJoinKeyPairs(templateCond, tLeftFieldCount);

        if (templatePairs.isEmpty()) {
            RexNode inst = instantiateRexNode(templateCond, null);
            return inst != null ? inst : templateCond;
        }

        RexBuilder rexBuilder = left.getCluster().getRexBuilder();
        List<RexNode> equalities = new ArrayList<>();

        for (int[] tPair : templatePairs) {
            int tLeftIdx = tPair[0];
            int tRightIdx = tPair[1] - tLeftFieldCount;

            List<String> tLeftFields = template.getLeft().getRowType().getFieldNames();
            List<String> tRightFields = template.getRight().getRowType().getFieldNames();

            String leftField = tLeftIdx < tLeftFields.size() ? tLeftFields.get(tLeftIdx) : null;
            String rightField = tRightIdx < tRightFields.size() ? tRightFields.get(tRightIdx) : null;

            int newLeftIdx = -1;
            int newRightIdx = -1;

            // Resolve left join key using interpretAttrs + registry
            if (leftField != null && SymbolKind.isSymbolName(leftField)) {
                List<ColumnRef> refs = interpretAttrs(Symbol.of(leftField));
                if (refs != null && !refs.isEmpty()) {
                    newLeftIdx = registry.resolveIndex(refs.get(0), left);
                }
            }

            // Resolve right join key using interpretAttrs + registry
            if (rightField != null && SymbolKind.isSymbolName(rightField)) {
                List<ColumnRef> refs = interpretAttrs(Symbol.of(rightField));
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
