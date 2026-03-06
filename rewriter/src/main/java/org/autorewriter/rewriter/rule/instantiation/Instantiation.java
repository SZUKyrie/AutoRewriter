package org.autorewriter.rewriter.rule.instantiation;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.*;
import org.apache.calcite.rel.type.*;
import org.apache.calcite.rex.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.util.ImmutableBitSet;
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
 * {@link Model} (bound during matching), and {@link Constraints} that provide
 * the instantiation mapping from target-side symbols to source-side symbols.
 *
 * <p>After a successful {@link Match}, the Model contains bound values for
 * source-side symbols. The Constraints provides the mapping from target-side
 * symbols to source-side symbols via {@link Constraints#instantiationOf}.
 * Instantiation walks the target template top-down, looks up each target
 * symbol's source symbol, retrieves the bound value from Model, and builds
 * new Calcite RelNode trees.
 */
public class Instantiation {

    /**
     * Instantiate a target template into a concrete {@link RelNode} tree using
     * the bindings from a successful match.
     *
     * @param targetTemplate the target-side template RelNode tree
     * @param model          the model populated during matching
     * @param constraints    the constraints providing instantiation mappings
     * @return a new RelNode tree with all placeholders resolved
     */
    public static RelNode instantiate(RelNode targetTemplate, Model model, Constraints constraints) {
        return instantiateNode(targetTemplate, model, constraints);
    }

    private static RelNode instantiateNode(RelNode template, Model model, Constraints constraints) {
        template = Match.unwrapHepVertex(template);

        // 1. INPUT (LogicalTableScan with t\d+ placeholder)
        if (template instanceof LogicalTableScan) {
            return instantiateInput((LogicalTableScan) template, model, constraints);
        }

        // 2. PROJ (LogicalProject)
        if (template instanceof LogicalProject) {
            return instantiateProject((LogicalProject) template, model, constraints);
        }

        // 3. FILTER (LogicalFilter) - could be simple or InSubFilter
        if (template instanceof LogicalFilter) {
            return instantiateFilter((LogicalFilter) template, model, constraints);
        }

        // 4. JOIN (LogicalJoin)
        if (template instanceof LogicalJoin) {
            return instantiateJoin((LogicalJoin) template, model, constraints);
        }

        // 5. AGG / DISTINCT (LogicalAggregate)
        if (template instanceof LogicalAggregate) {
            return instantiateAggregate((LogicalAggregate) template, model, constraints);
        }

        // Fallback: return template as-is
        return template;
    }

    // ── INPUT ──────────────────────────────────────────────────────────────

    private static RelNode instantiateInput(LogicalTableScan template, Model model, Constraints constraints) {
        String tableName = getTableName(template);
        if (SymbolKind.isSymbolName(tableName) && tableName.charAt(0) == 't') {
            Symbol targetSym = Symbol.of(tableName);
            Symbol sourceSym = constraints.instantiationOf(targetSym);
            if (sourceSym != null) {
                RelNode bound = model.ofTable(sourceSym);
                if (bound != null) return bound;
            }
            // If no instantiation mapping, try direct lookup
            RelNode direct = model.ofTable(targetSym);
            if (direct != null) return direct;
        }
        return template;
    }

    // ── PROJ ───────────────────────────────────────────────────────────────

    private static RelNode instantiateProject(LogicalProject template, Model model, Constraints constraints) {
        // Recursively instantiate child
        RelNode child = instantiateNode(template.getInput(), model, constraints);

        // Find attrs and schema placeholders in template field names
        List<String> templateFields = template.getRowType().getFieldNames();
        String attrsPlaceholder = null;
        String schemaPlaceholder = null;
        for (String field : templateFields) {
            if (SymbolKind.isSymbolName(field)) {
                if (field.charAt(0) == 'a') attrsPlaceholder = field;
                else if (field.charAt(0) == 's') schemaPlaceholder = field;
            }
        }

        // Resolve source attrs and schema via instantiation mapping
        List<ColumnRef> columnRefs = null;
        if (attrsPlaceholder != null) {
            Symbol targetAttrsSym = Symbol.of(attrsPlaceholder);
            Symbol sourceAttrsSym = constraints.instantiationOf(targetAttrsSym);
            if (sourceAttrsSym != null) {
                columnRefs = model.ofAttrs(sourceAttrsSym);
            }
            if (columnRefs == null) {
                columnRefs = model.ofAttrs(targetAttrsSym);
            }
        }

        RelDataType schema = null;
        if (schemaPlaceholder != null) {
            Symbol targetSchemaSym = Symbol.of(schemaPlaceholder);
            Symbol sourceSchemaSym = constraints.instantiationOf(targetSchemaSym);
            if (sourceSchemaSym != null) {
                schema = model.ofSchema(sourceSchemaSym);
            }
            if (schema == null) {
                schema = model.ofSchema(targetSchemaSym);
            }
        }

        if (columnRefs == null || columnRefs.isEmpty()) {
            // No bindings found - return child with identity projection
            return child;
        }

        // Try to get stored projection RexNodes
        String sourceAttrsName = attrsPlaceholder;
        Symbol targetAttrsSym = Symbol.of(attrsPlaceholder);
        Symbol sourceAttrsSym = constraints.instantiationOf(targetAttrsSym);
        if (sourceAttrsSym != null) {
            sourceAttrsName = sourceAttrsSym.name();
        }

        @SuppressWarnings("unchecked")
        List<RexNode> storedProjects = (List<RexNode>) model.ofExtra(sourceAttrsName + "_projects");

        // Build projection expressions
        RexBuilder rexBuilder = child.getCluster().getRexBuilder();
        List<RexNode> projects = new ArrayList<>();
        List<String> fieldNames = new ArrayList<>();

        if (storedProjects != null && storedProjects.size() == columnRefs.size()) {
            // Use stored projections, but rebind RexInputRef indices to new child
            for (int i = 0; i < storedProjects.size(); i++) {
                RexNode storedProject = storedProjects.get(i);
                RexNode rebound = rebindRexNode(storedProject, columnRefs.get(i), child, rexBuilder);
                projects.add(rebound);
            }
        } else {
            // Build simple RexInputRef projections from ColumnRefs
            for (ColumnRef ref : columnRefs) {
                int idx = ColumnRefResolver.resolveIndex(ref, child);
                if (idx >= 0) {
                    RelDataType fieldType = child.getRowType().getFieldList().get(idx).getType();
                    projects.add(rexBuilder.makeInputRef(fieldType, idx));
                } else {
                    // Column not found - try by name only
                    List<String> childFields = child.getRowType().getFieldNames();
                    boolean found = false;
                    for (int j = 0; j < childFields.size(); j++) {
                        if (childFields.get(j).equalsIgnoreCase(ref.getColumnName())) {
                            RelDataType fieldType = child.getRowType().getFieldList().get(j).getType();
                            projects.add(rexBuilder.makeInputRef(fieldType, j));
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        // Last resort: use first field
                        projects.add(rexBuilder.makeInputRef(
                                child.getRowType().getFieldList().get(0).getType(), 0));
                    }
                }
            }
        }

        // Build field names from schema or column refs
        if (schema != null) {
            fieldNames = new ArrayList<>(schema.getFieldNames());
        } else {
            for (ColumnRef ref : columnRefs) {
                fieldNames.add(ref.getColumnName());
            }
        }

        // Ensure field names list matches projects list size
        while (fieldNames.size() < projects.size()) {
            fieldNames.add("col" + fieldNames.size());
        }
        if (fieldNames.size() > projects.size()) {
            fieldNames = new ArrayList<>(fieldNames.subList(0, projects.size()));
        }

        return LogicalProject.create(child, Collections.emptyList(), projects, fieldNames);
    }

    // ── FILTER ─────────────────────────────────────────────────────────────

    private static RelNode instantiateFilter(LogicalFilter template, Model model, Constraints constraints) {
        // Check if InSubFilter
        if (hasRexSubQuery(template.getCondition())) {
            return instantiateInSubFilter(template, model, constraints);
        }

        // Simple filter
        RelNode child = instantiateNode(template.getInput(), model, constraints);

        // Get the predicate from model
        RexNode condition = instantiateRexNode(template.getCondition(), child, model, constraints);

        if (condition == null) {
            condition = template.getCondition();
        }

        return LogicalFilter.create(child, condition);
    }

    private static RelNode instantiateInSubFilter(LogicalFilter template, Model model, Constraints constraints) {
        // Instantiate main input
        RelNode child = instantiateNode(template.getInput(), model, constraints);

        // The subquery is inside the condition - instantiate it
        RexNode condition = instantiateRexNode(template.getCondition(), child, model, constraints);
        if (condition == null) {
            condition = template.getCondition();
        }

        return LogicalFilter.create(child, condition);
    }

    // ── JOIN ───────────────────────────────────────────────────────────────

    private static RelNode instantiateJoin(LogicalJoin template, Model model, Constraints constraints) {
        RelNode left = instantiateNode(template.getLeft(), model, constraints);
        RelNode right = instantiateNode(template.getRight(), model, constraints);

        // Rebuild join condition
        RexNode condition = rebuildJoinCondition(template, left, right, model, constraints);

        return LogicalJoin.create(left, right, Collections.emptyList(), condition,
                Collections.emptySet(), template.getJoinType());
    }

    private static RexNode rebuildJoinCondition(LogicalJoin template, RelNode left, RelNode right,
                                                Model model, Constraints constraints) {
        RexNode templateCond = template.getCondition();
        int tLeftFieldCount = template.getLeft().getRowType().getFieldCount();

        // Extract join key pairs from template condition
        List<int[]> templatePairs = extractJoinKeyPairs(templateCond, tLeftFieldCount);

        if (templatePairs.isEmpty()) {
            // Try to instantiate the condition directly
            RexNode inst = instantiateRexNode(templateCond, null, model, constraints);
            return inst != null ? inst : templateCond;
        }

        RexBuilder rexBuilder = left.getCluster().getRexBuilder();
        List<RexNode> equalities = new ArrayList<>();

        for (int[] tPair : templatePairs) {
            int tLeftIdx = tPair[0];
            int tRightIdx = tPair[1] - tLeftFieldCount;

            // Get placeholder names
            List<String> tLeftFields = template.getLeft().getRowType().getFieldNames();
            List<String> tRightFields = template.getRight().getRowType().getFieldNames();

            String leftField = tLeftIdx < tLeftFields.size() ? tLeftFields.get(tLeftIdx) : null;
            String rightField = tRightIdx < tRightFields.size() ? tRightFields.get(tRightIdx) : null;

            int newLeftIdx = -1;
            int newRightIdx = -1;

            // Resolve left join key
            if (leftField != null && SymbolKind.isSymbolName(leftField)) {
                Symbol targetSym = Symbol.of(leftField);
                Symbol sourceSym = constraints.instantiationOf(targetSym);
                List<ColumnRef> refs = sourceSym != null ? model.ofAttrs(sourceSym) : model.ofAttrs(targetSym);
                if (refs != null && !refs.isEmpty()) {
                    newLeftIdx = ColumnRefResolver.resolveIndex(refs.get(0), left);
                }
            }

            // Resolve right join key
            if (rightField != null && SymbolKind.isSymbolName(rightField)) {
                Symbol targetSym = Symbol.of(rightField);
                Symbol sourceSym = constraints.instantiationOf(targetSym);
                List<ColumnRef> refs = sourceSym != null ? model.ofAttrs(sourceSym) : model.ofAttrs(targetSym);
                if (refs != null && !refs.isEmpty()) {
                    newRightIdx = ColumnRefResolver.resolveIndex(refs.get(0), right);
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

        if (equalities.isEmpty()) {
            return rexBuilder.makeLiteral(true);
        }
        if (equalities.size() == 1) {
            return equalities.get(0);
        }
        return rexBuilder.makeCall(SqlStdOperatorTable.AND, equalities);
    }

    // ── AGGREGATE ──────────────────────────────────────────────────────────

    private static RelNode instantiateAggregate(LogicalAggregate template, Model model, Constraints constraints) {
        RelNode child = instantiateNode(template.getInput(), model, constraints);

        // Resolve group-by attrs
        List<String> templateFields = template.getRowType().getFieldNames();
        ImmutableBitSet groupSet = template.getGroupSet();

        // If template has placeholder attrs, resolve them
        for (String field : templateFields) {
            if (SymbolKind.isSymbolName(field) && field.charAt(0) == 'a') {
                Symbol targetSym = Symbol.of(field);
                Symbol sourceSym = constraints.instantiationOf(targetSym);
                List<ColumnRef> refs = sourceSym != null ? model.ofAttrs(sourceSym) : model.ofAttrs(targetSym);
                if (refs != null) {
                    ImmutableBitSet.Builder builder = ImmutableBitSet.builder();
                    for (ColumnRef ref : refs) {
                        int idx = ColumnRefResolver.resolveIndex(ref, child);
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

    // ── RexNode instantiation ──────────────────────────────────────────────

    private static RexNode instantiateRexNode(RexNode template, RelNode context,
                                              Model model, Constraints constraints) {
        if (template instanceof RexSubQuery) {
            RexSubQuery sub = (RexSubQuery) template;
            RelNode newRel = instantiateNode(sub.rel, model, constraints);
            // Rebuild operands with correct types from the context (filter input)
            List<RexNode> newOperands = new ArrayList<>();
            for (RexNode operand : sub.getOperands()) {
                RexNode inst = instantiateRexNode(operand, context, model, constraints);
                newOperands.add(inst != null ? inst : operand);
            }
            // Always rebuild with both new rel and new operands
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
                    return bound;
                }
            }

            // Recursively instantiate operands
            List<RexNode> newOperands = new ArrayList<>();
            boolean changed = false;
            for (RexNode operand : call.getOperands()) {
                RexNode inst = instantiateRexNode(operand, context, model, constraints);
                if (inst != null && inst != operand) {
                    newOperands.add(inst);
                    changed = true;
                } else {
                    newOperands.add(operand);
                }
            }
            if (changed) {
                return call.clone(call.getType(), newOperands);
            }
            return call;
        }

        // RexInputRef: rebind to the context node's column type
        // (template placeholder types like JavaType(String) must be replaced
        // with the actual column type from the instantiated child)
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

    // Rebind a single stored projection RexNode to a new child
    private static RexNode rebindRexNode(RexNode expr, ColumnRef ref, RelNode child, RexBuilder rexBuilder) {
        if (expr instanceof RexInputRef) {
            int idx = ColumnRefResolver.resolveIndex(ref, child);
            if (idx >= 0) {
                RelDataType fieldType = child.getRowType().getFieldList().get(idx).getType();
                return rexBuilder.makeInputRef(fieldType, idx);
            }
            // Fallback: try name matching
            List<String> fieldNames = child.getRowType().getFieldNames();
            for (int i = 0; i < fieldNames.size(); i++) {
                if (fieldNames.get(i).equalsIgnoreCase(ref.getColumnName())) {
                    RelDataType fieldType = child.getRowType().getFieldList().get(i).getType();
                    return rexBuilder.makeInputRef(fieldType, i);
                }
            }
        }
        return expr;
    }

    // ── Utilities ──────────────────────────────────────────────────────────

    private static String getTableName(LogicalTableScan scan) {
        List<String> names = scan.getTable().getQualifiedName();
        return names.get(names.size() - 1);
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
