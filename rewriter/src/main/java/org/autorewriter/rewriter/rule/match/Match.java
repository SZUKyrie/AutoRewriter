package org.autorewriter.rewriter.rule.match;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.*;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rex.*;
import org.autorewriter.rewriter.optimize.costBaseOpt.insub.LogicalInSubFilter;
import org.autorewriter.rewriter.rule.constraint.Constraint;
import org.autorewriter.rewriter.rule.constraint.ConstraintKind;
import org.autorewriter.rewriter.rule.constraint.Constraints;
import org.autorewriter.rewriter.rule.model.Model;
import org.autorewriter.rewriter.rule.symbol.*;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.autorewriter.rewriter.rule.util.ColumnRefResolver;

import java.util.*;

/**
 * Main recursive matching engine.
 *
 * <p>Matches a source template {@link RelNode} against a query {@link RelNode},
 * binding symbols in the {@link Model}. This is the WeTune-style matching
 * algorithm adapted for Calcite.
 *
 * <p>Template placeholder conventions (from ShardingSphere parser):
 * <ul>
 *   <li>{@code t\d+} in LogicalTableScan table name &rarr; TABLE symbol (wildcard for any subtree)</li>
 *   <li>{@code a\d+} in field names &rarr; ATTRS symbol (column references)</li>
 *   <li>{@code p\d+} in RexCall operator name &rarr; PRED symbol (predicate expression)</li>
 *   <li>{@code s\d+} in field names &rarr; SCHEMA symbol (output schema)</li>
 * </ul>
 */
@Slf4j
public class Match {

    /**
     * Match a source template against a query RelNode, binding symbols in the model.
     * Returns true if matching succeeds.
     */
    public static boolean match(RelNode template, RelNode query, Model model) {
        // Unwrap HepRelVertex only (NOT RelSubset — handled below)
        while (query instanceof HepRelVertex) {
            query = ((HepRelVertex) query).getCurrentRel();
        }

        // Handle VolcanoPlanner's RelSubset: try matching against each alternative
        // in the equivalence set. This is critical for rule chaining — e.g., Rule 1
        // must see Rule 0's output (InnerJoin) which is a different alternative in
        // the same RelSubset as the original InSubFilter.
        if (query instanceof RelSubset) {
            RelSubset subset = (RelSubset) query;
            for (RelNode rel : subset.getRels()) {
                if (rel instanceof RelSubset) continue;
                Model derived = model.derive();
                if (match(template, rel, derived)) {
                    // Found a matching alternative — replay into the real model
                    return match(template, rel, model);
                }
            }
            return false;
        }

        // 1. If template is a LogicalTableScan with placeholder name (t\d+) = INPUT
        //    It can match ANY query RelNode subtree (wildcard)
        if (template instanceof LogicalTableScan) {
            String tableName = getTableName((LogicalTableScan) template);
            if (SymbolKind.isSymbolName(tableName) && tableName.charAt(0) == 't') {
                Symbol sym = Symbol.of(tableName);
                if (!model.assign(sym, query)) return false;
                return model.checkConstraints();
            }
            // Not a placeholder - exact match
            if (!(query instanceof LogicalTableScan)) return false;
            return tableName.equals(getTableName((LogicalTableScan) query));
        }

        // 2. LogicalAggregate (PROJ* / DISTINCT)
        if (template instanceof LogicalAggregate) {
            if (!(query instanceof LogicalAggregate)) return false;
            return matchAggregate((LogicalAggregate) template, (LogicalAggregate) query, model);
        }

        // 3. LogicalProject (PROJ)
        if (template instanceof LogicalProject) {
            if (!(query instanceof LogicalProject)) return false;
            return matchProject((LogicalProject) template, (LogicalProject) query, model);
        }

        // 4. LogicalInSubFilter — bind lhsRef attrs symbol (WeTune matchInSub)
        if (template instanceof LogicalInSubFilter) {
            if (!(query instanceof LogicalInSubFilter)) return false;
            return matchInSubFilter((LogicalInSubFilter) template, (LogicalInSubFilter) query, model);
        }

        // 5. LogicalFilter (also handles LogicalInSubFilter as part of filter chain)
        if (template instanceof LogicalFilter) {
            if (query instanceof LogicalFilter) {
                return matchFilter((LogicalFilter) template, (LogicalFilter) query, model);
            }
            if (query instanceof LogicalInSubFilter) {
                // InSubFilter is part of the filter chain — use chain matching
                return FilterMatcher.matchFilterChainFromInSub(
                        (LogicalFilter) template, (LogicalInSubFilter) query, model);
            }
            return false;
        }

        // 5. LogicalJoin
        if (template instanceof LogicalJoin) {
            if (!(query instanceof LogicalJoin)) return false;
            return matchJoin((LogicalJoin) template, (LogicalJoin) query, model);
        }

        // Default: class must match, then recurse on children
        if (!template.getClass().equals(query.getClass())) return false;
        if (template.getInputs().size() != query.getInputs().size()) return false;
        for (int i = 0; i < template.getInputs().size(); i++) {
            if (!match(template.getInput(i), query.getInput(i), model)) return false;
        }
        return true;
    }

    // ── Project matching ──────────────────────────────────────────────────

    private static boolean matchProject(LogicalProject template, LogicalProject query, Model model) {
        // First recurse on child
        if (!match(template.getInput(), query.getInput(), model)) return false;

        // Extract placeholder names from template field names
        List<String> templateFields = template.getRowType().getFieldNames();

        // Find attrs placeholder (a\d+) and schema placeholder (s\d+)
        String attrsPlaceholder = null;
        String schemaPlaceholder = null;
        for (String field : templateFields) {
            if (SymbolKind.isSymbolName(field)) {
                if (field.charAt(0) == 'a') attrsPlaceholder = field;
                else if (field.charAt(0) == 's') schemaPlaceholder = field;
            }
        }

        // Bind attrs: resolve all query projection columns to ColumnRefs
        if (attrsPlaceholder != null) {
            Symbol attrsSym = Symbol.of(attrsPlaceholder);
            List<ColumnRef> columnRefs = new ArrayList<>();
            RelNode queryInput = unwrapHepVertex(query.getInput());
            for (int i = 0; i < query.getProjects().size(); i++) {
                RexNode project = query.getProjects().get(i);
                if (project instanceof RexInputRef) {
                    int idx = ((RexInputRef) project).getIndex();
                    ColumnRef ref = ColumnRefResolver.resolve(idx, queryInput);
                    columnRefs.add(ref);
                } else {
                    // For complex expressions, use the output field name
                    String fieldName = query.getRowType().getFieldNames().get(i);
                    columnRefs.add(new ColumnRef("$expr", fieldName));
                }
            }
            if (!model.assign(attrsSym, columnRefs)) return false;

            // Also store the projection RexNodes for later use in Instantiation
            model.putExtra(attrsPlaceholder + "_projects", new ArrayList<>(query.getProjects()));
        }

        // Bind schema
        if (schemaPlaceholder != null) {
            Symbol schemaSym = Symbol.of(schemaPlaceholder);
            if (!model.assign(schemaSym, query.getRowType())) return false;
        }

        return model.checkConstraints();
    }

    // ── Transparent Project matching ─────────────────────────────────────

    /**
     * Match a template LogicalProject "transparently" against a non-Project query node.
     *
     * <p>WeTune rule templates use {@code Proj<a s>} as boundary markers between Join
     * and Filter/Input. In RBO (HepPlanner), {@code wrapWithIdentityProjectIfNeeded}
     * creates matching Project nodes. In CBO (VolcanoPlanner), identity projections
     * are eliminated by the MEMO, so the template Proj has no corresponding query node.
     *
     * <p>This method "skips" the Proj layer: it binds attrs/schema from the query's
     * output columns (as if an identity projection existed) and recurses into the
     * Proj's child template against the query directly.
     *
     * <p>Only activates for WeTune template Projects (field names are symbol placeholders).
     * Non-template Projects (real query projections) still require strict type matching.
     */
    private static boolean matchTransparentProject(LogicalProject template, RelNode query, Model model) {
        // Extract placeholder names from template field names
        List<String> templateFields = template.getRowType().getFieldNames();
        String attrsPlaceholder = null;
        String schemaPlaceholder = null;
        for (String field : templateFields) {
            if (SymbolKind.isSymbolName(field)) {
                if (field.charAt(0) == 'a') attrsPlaceholder = field;
                else if (field.charAt(0) == 's') schemaPlaceholder = field;
            }
        }

        // Only activate for WeTune template Projects (must have at least one symbol placeholder)
        if (attrsPlaceholder == null && schemaPlaceholder == null) {
            return false;
        }

        // Recurse: match the Proj's child template against the query directly
        if (!match(template.getInput(), query, model)) return false;

        // Bind attrs: resolve all output columns of the query as ColumnRefs
        // (equivalent to what an identity projection would produce)
        if (attrsPlaceholder != null) {
            Symbol attrsSym = Symbol.of(attrsPlaceholder);
            List<ColumnRef> columnRefs = new ArrayList<>();
            RelNode resolveTarget = unwrapHepVertex(query);
            for (int i = 0; i < query.getRowType().getFieldCount(); i++) {
                ColumnRef ref = ColumnRefResolver.resolve(i, resolveTarget);
                columnRefs.add(ref);
            }
            if (!model.assign(attrsSym, columnRefs)) return false;
        }

        // Bind schema
        if (schemaPlaceholder != null) {
            Symbol schemaSym = Symbol.of(schemaPlaceholder);
            if (!model.assign(schemaSym, query.getRowType())) return false;
        }

        return model.checkConstraints();
    }

    // ── Aggregate matching (DISTINCT / Proj*) ─────────────────────────────

    private static boolean matchAggregate(LogicalAggregate template, LogicalAggregate query, Model model) {
        // Both must be DISTINCT (empty agg call lists)
        if (!template.getAggCallList().isEmpty() || !query.getAggCallList().isEmpty()) return false;

        // Recurse on child
        if (!match(template.getInput(), query.getInput(), model)) return false;

        // Extract attrs placeholder from template field names
        List<String> templateFields = template.getRowType().getFieldNames();
        for (String field : templateFields) {
            if (SymbolKind.isSymbolName(field) && field.charAt(0) == 'a') {
                Symbol attrsSym = Symbol.of(field);
                // Resolve query's group-by columns to ColumnRefs
                RelNode queryInput = unwrapHepVertex(query.getInput());
                List<ColumnRef> columnRefs = new ArrayList<>();
                for (int idx : query.getGroupSet()) {
                    ColumnRef ref = ColumnRefResolver.resolve(idx, queryInput);
                    columnRefs.add(ref);
                }
                if (!model.assign(attrsSym, columnRefs)) return false;
                break; // only one attrs placeholder expected
            }
        }

        // Schema placeholder
        for (String field : templateFields) {
            if (SymbolKind.isSymbolName(field) && field.charAt(0) == 's') {
                Symbol schemaSym = Symbol.of(field);
                if (!model.assign(schemaSym, query.getRowType())) return false;
                break;
            }
        }

        return model.checkConstraints();
    }

    // ── Filter matching ───────────────────────────────────────────────────

    // ── LogicalInSubFilter matching (WeTune matchInSub) ─────────────────

    /**
     * Match a template LogicalInSubFilter against a query LogicalInSubFilter.
     * Binds the lhsRef's corresponding attrs symbol, aligned with WeTune's
     * {@code Match.matchInSub()} which extracts {@code InSubNode.expr()}
     * value references and assigns them to the attrs symbol.
     */
    private static boolean matchInSubFilter(LogicalInSubFilter template,
                                             LogicalInSubFilter query, Model model) {
        // 1. Recursively match left (filtered input) and right (subquery)
        if (!match(template.getLeft(), query.getLeft(), model)) return false;
        if (!match(template.getRight(), query.getRight(), model)) return false;

        // 2. Extract and bind the lhsRef attrs symbol
        // In WeTune, InSubFilter<a> means `a` is the column(s) on the LHS of the IN check.
        // The template's lhsRef is a RexInputRef whose index maps to a field name
        // in the left child's row type. If that field name is a symbol (a\d+),
        // bind it to the query's lhsRef resolved as a ColumnRef.
        RexNode tLhsRef = template.getLhsRef();
        if (tLhsRef instanceof RexInputRef) {
            int tIdx = ((RexInputRef) tLhsRef).getIndex();
            RelNode tLeft = unwrapHepVertex(template.getLeft());
            List<String> tLeftFields = tLeft.getRowType().getFieldNames();
            if (tIdx < tLeftFields.size()) {
                String fieldName = tLeftFields.get(tIdx);
                if (SymbolKind.isSymbolName(fieldName) && fieldName.charAt(0) == 'a') {
                    // Resolve query's lhsRef to ColumnRef
                    RexNode qLhsRef = query.getLhsRef();
                    if (qLhsRef instanceof RexInputRef) {
                        int qIdx = ((RexInputRef) qLhsRef).getIndex();
                        RelNode qLeft = unwrapHepVertex(query.getLeft());
                        ColumnRef ref = ColumnRefResolver.resolve(qIdx, qLeft);
                        List<ColumnRef> refs = new ArrayList<>();
                        refs.add(ref);
                        if (!model.assign(Symbol.of(fieldName), refs)) return false;
                    }
                }
            }
        }

        return model.checkConstraints();
    }

    private static boolean matchFilter(LogicalFilter template, LogicalFilter query, Model model) {
        RexNode templateCond = template.getCondition();

        // Check if this is an InSubFilter (template condition contains RexSubQuery)
        if (hasRexSubQuery(templateCond)) {
            return matchInSubFilter(template, query, model);
        }

        // Use filter chain matching when EITHER the template OR the query has
        // multiple consecutive filter-like nodes (LogicalFilter or LogicalInSubFilter).
        RelNode templateInput = unwrapHepVertex(template.getInput());
        RelNode queryInput = unwrapHepVertex(query.getInput());
        if (templateInput instanceof LogicalFilter || templateInput instanceof LogicalInSubFilter
                || queryInput instanceof LogicalFilter || queryInput instanceof LogicalInSubFilter
                || containsFilterAlternative(query.getInput())) {
            return FilterMatcher.matchFilterChain(template, query, model);
        }

        // Simple single filter matching
        // Recurse on child first
        if (!match(template.getInput(), query.getInput(), model)) return false;

        // Match the filter condition
        if (!matchRexNode(templateCond, query.getCondition(), template, query, model)) return false;

        return model.checkConstraints();
    }

    // ── InSubFilter matching ──────────────────────────────────────────────

    private static boolean matchInSubFilter(LogicalFilter template, LogicalFilter query, Model model) {
        // Extract RexSubQuery from both conditions
        RexSubQuery templateSubQuery = extractRexSubQuery(template.getCondition());
        if (templateSubQuery == null) return false;

        RexSubQuery querySubQuery = extractRexSubQuery(query.getCondition());
        if (querySubQuery == null) return false;

        // Match main input
        if (!match(template.getInput(), query.getInput(), model)) return false;

        // Match subquery RelNode
        if (!match(templateSubQuery.rel, querySubQuery.rel, model)) return false;

        // Bind attrs for the correlation columns from template's operands
        for (RexNode operand : templateSubQuery.operands) {
            if (operand instanceof RexInputRef) {
                int templateIdx = ((RexInputRef) operand).getIndex();
                List<String> inputFieldNames = template.getInput().getRowType().getFieldNames();
                if (templateIdx < inputFieldNames.size()) {
                    String fieldName = inputFieldNames.get(templateIdx);
                    if (SymbolKind.isSymbolName(fieldName) && fieldName.charAt(0) == 'a') {
                        Symbol attrsSym = Symbol.of(fieldName);
                        List<ColumnRef> columnRefs = new ArrayList<>();
                        for (RexNode qOp : querySubQuery.operands) {
                            if (qOp instanceof RexInputRef) {
                                int qIdx = ((RexInputRef) qOp).getIndex();
                                ColumnRef ref = ColumnRefResolver.resolve(qIdx, unwrapHepVertex(query.getInput()));
                                columnRefs.add(ref);
                            }
                        }
                        if (!model.assign(attrsSym, columnRefs)) return false;
                    }
                }
            }
        }

        return model.checkConstraints();
    }

    // ── Join matching ─────────────────────────────────────────────────────

    private static boolean matchJoin(LogicalJoin template, LogicalJoin query, Model model) {
        // Check join type
        if (template.getJoinType() != query.getJoinType()) return false;

        // Try direct match first (left-to-left, right-to-right)
        Model directModel = model.derive();
        if (matchJoinWithSides(template, query, directModel, false)) {
            // Replay successful assignments into the parent model
            return matchJoinWithSides(template, query, model, false);
        }

        // For INNER JOIN, try flipped match (left-to-right, right-to-left)
        if (template.getJoinType() == JoinRelType.INNER) {
            Model flippedModel = model.derive();
            if (matchJoinWithSides(template, query, flippedModel, true)) {
                return matchJoinWithSides(template, query, model, true);
            }
        }

        return false;
    }

    private static boolean matchJoinWithSides(LogicalJoin template, LogicalJoin query,
                                               Model model, boolean flipped) {
        RelNode tLeft = template.getLeft();
        RelNode tRight = template.getRight();
        RelNode qLeft = flipped ? query.getRight() : query.getLeft();
        RelNode qRight = flipped ? query.getLeft() : query.getRight();

        if (!matchJoinChild(tLeft, qLeft, model)) return false;
        if (!matchJoinChild(tRight, qRight, model)) return false;

        // Match join condition
        if (!matchJoinCondition(template, query, model, flipped)) return false;

        return model.checkConstraints();
    }

    /**
     * Match a Join child: try standard matching first, then transparent Proj matching.
     *
     * <p>WeTune rule templates have {@code Proj<a s>(Filter<p a>(Input<t>))} as Join children,
     * but in CBO the identity Proj is eliminated by the MEMO. This method falls back to
     * {@link #matchTransparentProject} when the template child is a LogicalProject but the
     * query child is not. This is the ONLY context where transparent Proj matching is allowed
     * — it does not apply at the top level or in non-Join contexts.
     */
    private static boolean matchJoinChild(RelNode template, RelNode query, Model model) {
        // Standard matching first
        if (match(template, query, model)) return true;

        // Transparent Proj fallback: only when template is Proj and standard match failed
        if (template instanceof LogicalProject) {
            RelNode unwrapped = unwrapHepVertex(template);
            if (unwrapped instanceof LogicalProject) {
                return matchTransparentProject((LogicalProject) unwrapped, query, model);
            }
        }
        return false;
    }

    /**
     * Match join conditions and bind join key attrs symbols.
     *
     * Strategy:
     * 1. Extract equi-join key pairs from QUERY condition using Calcite
     * 2. Find join key symbols from constraints' AttrsSub relationships
     * 3. If no constraints available, extract from template children's field names
     * 4. Bind query key ColumnRefs to the found symbols
     */
    private static boolean matchJoinCondition(LogicalJoin template, LogicalJoin query, Model model, boolean flipped) {
        RexNode queryCond = query.getCondition();
        int qLeftFieldCount = query.getLeft().getRowType().getFieldCount();

        List<int[]> queryPairs = extractJoinKeyPairs(queryCond, qLeftFieldCount);
        if (queryPairs.isEmpty()) {
            return matchRexNode(template.getCondition(), queryCond, template, query, model);
        }

        RelNode qLeftChild = unwrapHepVertex(flipped ? query.getRight() : query.getLeft());
        RelNode qRightChild = unwrapHepVertex(flipped ? query.getLeft() : query.getRight());

        // Find join key symbols: first try constraints, then template children field names
        Constraints constraints = model.constraints();
        RelNode tLeft = unwrapHepVertex(template.getLeft());
        RelNode tRight = unwrapHepVertex(template.getRight());

        Symbol leftKeySym = findJoinKeySymbol(constraints, tLeft);
        Symbol rightKeySym = findJoinKeySymbol(constraints, tRight);

        for (int[] qPair : queryPairs) {
            int qLeftIdx = flipped ? (qPair[1] - qLeftFieldCount) : qPair[0];
            int qRightIdx = flipped ? qPair[0] : (qPair[1] - qLeftFieldCount);

            if (leftKeySym != null) {
                ColumnRef leftRef = ColumnRefResolver.resolve(qLeftIdx, qLeftChild);
                if (!model.assign(leftKeySym, Collections.singletonList(leftRef))) return false;
            }

            if (rightKeySym != null) {
                ColumnRef rightRef = ColumnRefResolver.resolve(qRightIdx, qRightChild);
                if (!model.assign(rightKeySym, Collections.singletonList(rightRef))) return false;
            }
        }

        return true;
    }

    /**
     * Find the attrs symbol for a join child — the column used as the equi-join key.
     *
     * Strategy:
     * 1. For Proj children: prefer schema-based (s\d+) AttrsSub lookup, since the
     *    Proj's output schema defines the join key, not the underlying table.
     *    The schema symbol (e.g., s0) is NOT embedded in the Proj's field names —
     *    it only appears in AttrsSub constraints like AttrsSub(a2, s0).
     * 2. Otherwise: find the child's table symbol (t\d+) and look up AttrsSub
     * 3. Fallback: use the child's first a\d+ field name
     */
    private static Symbol findJoinKeySymbol(Constraints constraints, RelNode child) {
        child = unwrapHepVertex(child);

        if (constraints != null) {
            // Strategy 1: For Proj nodes, prefer schema-based AttrsSub lookup.
            // Template Proj<a1 s0> loses the s0 metadata in the Calcite LogicalProject,
            // but AttrsSub(a2, s0) still records which attrs belong to the schema.
            if (child instanceof LogicalProject) {
                for (Constraint c : constraints.ofKind(ConstraintKind.ATTRS_SUB)) {
                    Symbol[] syms = c.symbols();
                    String name = syms[1].name();
                    if (name.charAt(0) == 's' && SymbolKind.isSymbolName(name)) {
                        return syms[0];
                    }
                }
            }

            // Strategy 2: Use constraints (AttrsSub) with table symbol
            String tableOrSchema = findTableOrSchemaSymbol(child);
            if (tableOrSchema != null) {
                Symbol tableSym = Symbol.of(tableOrSchema);
                for (Constraint c : constraints.ofKind(ConstraintKind.ATTRS_SUB)) {
                    Symbol[] syms = c.symbols();
                    if (syms[1].equals(tableSym)) {
                        return syms[0];
                    }
                }
            }
        }

        // Strategy 3: First a\d+ field name from child (for simple cases like Input<t0>)
        List<String> fieldNames = child.getRowType().getFieldNames();
        for (String fn : fieldNames) {
            if (SymbolKind.isSymbolName(fn) && fn.charAt(0) == 'a') {
                return Symbol.of(fn);
            }
        }

        return null;
    }

    /**
     * Find the table symbol (t\d+) or schema symbol (s\d+) for a template node.
     */
    private static String findTableOrSchemaSymbol(RelNode node) {
        node = unwrapHepVertex(node);
        // For LogicalTableScan: table name is the symbol
        if (node instanceof LogicalTableScan) {
            List<String> names = node.getTable().getQualifiedName();
            String name = names.get(names.size() - 1);
            if (SymbolKind.isSymbolName(name)) return name;
        }
        // For LogicalProject: look for schema symbol (s\d+) in field names
        if (node instanceof LogicalProject) {
            List<String> fieldNames = node.getRowType().getFieldNames();
            for (String fn : fieldNames) {
                if (SymbolKind.isSymbolName(fn) && fn.charAt(0) == 's') return fn;
            }
        }
        // Recurse into first child
        if (node.getInputs().size() > 0) {
            return findTableOrSchemaSymbol(node.getInput(0));
        }
        return null;
    }

    // ── RexNode matching ──────────────────────────────────────────────────

    /**
     * Match a template RexNode against a query RexNode, binding symbols.
     * The {@code templateOperator} provides template context for extracting attrs placeholders.
     * The {@code queryOperator} provides context for resolving column references.
     */
    static boolean matchRexNode(RexNode template, RexNode query, RelNode templateOperator, RelNode queryOperator, Model model) {
        if (template instanceof RexCall && query instanceof RexCall) {
            RexCall tCall = (RexCall) template;
            RexCall qCall = (RexCall) query;

            // Check if template operator is a predicate placeholder (p\d+)
            String opName = tCall.getOperator().getName();
            if (SymbolKind.isSymbolName(opName) && opName.charAt(0) == 'p') {
                Symbol predSym = Symbol.of(opName);
                if (!model.assign(predSym, query)) return false;

                // Store the source context so Instantiation can rebind RexInputRef
                // indices when the predicate is placed in a different structural context
                // (e.g., from Filter(t1) to Filter(InnerJoin(t0, t1)))
                RelNode resolveTarget = getFilterInput(queryOperator);
                model.putExtra(predSym.name() + "_context", resolveTarget);

                // Collect all column refs from the query predicate and bind attrs
                List<ColumnRef> columnRefs = collectColumnRefs(query, resolveTarget);

                // Find the attrs placeholder associated with this predicate
                // from the template's operands (RexInputRef pointing to template filter's input fields)
                RelNode templateResolveTarget = getFilterInput(templateOperator);
                for (RexNode operand : tCall.getOperands()) {
                    if (operand instanceof RexInputRef) {
                        int fieldIdx = ((RexInputRef) operand).getIndex();
                        // Look up the field name in the TEMPLATE operator's input
                        List<String> templateFieldNames = templateResolveTarget.getRowType().getFieldNames();
                        if (fieldIdx < templateFieldNames.size()) {
                            String fieldName = templateFieldNames.get(fieldIdx);
                            if (SymbolKind.isSymbolName(fieldName) && fieldName.charAt(0) == 'a') {
                                Symbol attrsSym = Symbol.of(fieldName);
                                if (!model.assign(attrsSym, columnRefs)) return false;
                            }
                        }
                    }
                }
                return true;
            }

            // Normal RexCall: operator and operand count must match
            if (!tCall.getOperator().equals(qCall.getOperator())) return false;
            if (tCall.getOperands().size() != qCall.getOperands().size()) return false;
            for (int i = 0; i < tCall.getOperands().size(); i++) {
                if (!matchRexNode(tCall.getOperands().get(i), qCall.getOperands().get(i), templateOperator, queryOperator, model)) {
                    return false;
                }
            }
            return true;
        }

        if (template instanceof RexSubQuery && query instanceof RexSubQuery) {
            RexSubQuery tSub = (RexSubQuery) template;
            RexSubQuery qSub = (RexSubQuery) query;

            if (!tSub.getOperator().equals(qSub.getOperator())) return false;

            if (tSub.getOperands().size() != qSub.getOperands().size()) return false;
            for (int i = 0; i < tSub.getOperands().size(); i++) {
                if (!matchRexNode(tSub.getOperands().get(i), qSub.getOperands().get(i), templateOperator, queryOperator, model)) {
                    return false;
                }
            }
            // Recurse into the sub-query RelNode
            return match(tSub.rel, qSub.rel, model);
        }

        if (template instanceof RexInputRef && query instanceof RexInputRef) {
            // InputRef matching always succeeds; binding happens through parent
            return true;
        }

        if (template instanceof RexLiteral && query instanceof RexLiteral) {
            return template.equals(query);
        }

        // Fallback: string comparison
        return template.toString().equals(query.toString());
    }

    // ── Utility methods ───────────────────────────────────────────────────

    /**
     * Unwrap planner-internal wrappers to get the real underlying RelNode.
     * Handles HepPlanner's HepRelVertex. For VolcanoPlanner's RelSubset,
     * see the handling in {@link #match} which iterates alternatives.
     */
    public static RelNode unwrapHepVertex(RelNode node) {
        while (node instanceof HepRelVertex) {
            node = ((HepRelVertex) node).getCurrentRel();
        }
        // For RelSubset, return the original logical node as a best-effort
        // default for non-match call sites (matchProject, matchJoin, etc.).
        // Match.match() handles full iteration BEFORE calling this method.
        if (node instanceof RelSubset) {
            RelSubset subset = (RelSubset) node;
            RelNode original = subset.getOriginal();
            if (original != null) {
                return original;
            }
        }
        return node;
    }

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

    private static RexSubQuery extractRexSubQuery(RexNode node) {
        if (node instanceof RexSubQuery) return (RexSubQuery) node;
        if (node instanceof RexCall) {
            for (RexNode operand : ((RexCall) node).getOperands()) {
                RexSubQuery sub = extractRexSubQuery(operand);
                if (sub != null) return sub;
            }
        }
        return null;
    }

    /**
     * Extract equi-join key pairs from a join condition.
     * Each pair is [leftIdx, rightIdx] where rightIdx includes the leftFieldCount offset.
     */
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
                    // Normalize: left from LHS, right from RHS
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
     * Collect all column references from a RexNode expression tree.
     */
    private static List<ColumnRef> collectColumnRefs(RexNode expr, RelNode resolveTarget) {
        List<ColumnRef> refs = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        collectColumnRefsRecursive(expr, resolveTarget, refs, seen);
        return refs;
    }

    private static void collectColumnRefsRecursive(RexNode expr, RelNode resolveTarget,
                                                    List<ColumnRef> refs, Set<Integer> seen) {
        if (expr instanceof RexInputRef) {
            int idx = ((RexInputRef) expr).getIndex();
            if (seen.add(idx)) {
                ColumnRef ref = ColumnRefResolver.resolve(idx, resolveTarget);
                refs.add(ref);
            }
        } else if (expr instanceof RexCall) {
            for (RexNode operand : ((RexCall) expr).getOperands()) {
                collectColumnRefsRecursive(operand, resolveTarget, refs, seen);
            }
        } else if (expr instanceof RexSubQuery) {
            for (RexNode operand : ((RexSubQuery) expr).getOperands()) {
                collectColumnRefsRecursive(operand, resolveTarget, refs, seen);
            }
        }
    }

    /**
     * For a filter operator, returns its input (the node whose row type the condition references).
     * For other operators, returns the operator itself.
     */
    private static RelNode getFilterInput(RelNode operator) {
        RelNode unwrapped = unwrapHepVertex(operator);
        if (unwrapped instanceof LogicalFilter) {
            return unwrapHepVertex(((LogicalFilter) unwrapped).getInput());
        }
        return unwrapped;
    }

    /**
     * Check if a RelNode (possibly a RelSubset) contains a LogicalFilter or
     * LogicalInSubFilter alternative. This handles the VolcanoPlanner case where
     * a filter chain's inner filter is wrapped in a RelSubset and getOriginal()
     * may return a non-filter node (e.g., the InnerJoin that was registered first).
     */
    static boolean containsFilterAlternative(RelNode node) {
        if (node instanceof RelSubset) {
            for (RelNode rel : ((RelSubset) node).getRels()) {
                if (rel instanceof RelSubset) continue;
                if (rel instanceof LogicalFilter || rel instanceof LogicalInSubFilter) {
                    return true;
                }
            }
        }
        return false;
    }
}
