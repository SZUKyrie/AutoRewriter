package org.autorewriter.rewriter.rule;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.*;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexCall;
import org.apache.shardingsphere.sql.parser.api.ASTNode;
import org.autorewriter.rewriter.analyze.RuleAnalysisContext;

import java.util.*;

@Slf4j
public class AutoRewriteRule extends RelOptRule {

    private final RelNode sourceTemplate;
    private final RelNode targetTemplate;
    private final List<ASTNode> matchConstraints;
    private final List<ASTNode> rewriteConstraints;

    private final Map<String, Object> placeholderBindings;

    public AutoRewriteRule(RelOptRuleOperand operand, RuleAnalysisContext ruleContext) {
        super(operand);
        this.sourceTemplate = ruleContext.getSourceRelNode();
        this.targetTemplate = ruleContext.getTargetRelNode();
        this.matchConstraints = ruleContext.getMatchConstraints();
        this.rewriteConstraints = ruleContext.getRewriteConstraints();
        this.placeholderBindings = new HashMap<>();
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
        RelNode queryNode = call.rel(0);
        placeholderBindings.clear();

        if (!recursiveMatch(sourceTemplate, queryNode, placeholderBindings)) {
            return false;
        }

        return checkMatchConstraints(placeholderBindings);
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
        log.debug("Rule matched, bindings: {}", placeholderBindings.keySet());

        Map<String, Object> rewriteBindings = applyRewriteConstraints(placeholderBindings);
        log.debug("Rewrite bindings: {}", rewriteBindings.keySet());

        RelNode rewrittenNode = fillTargetTemplate(targetTemplate, rewriteBindings);
        log.info("Query rewritten: {} -> {}",
            call.rel(0).getClass().getSimpleName(),
            rewrittenNode.getClass().getSimpleName());

        // Adjust row type to match original node if needed (e.g., LEFT JOIN -> INNER JOIN changes nullability)
        RelNode originalNode = call.rel(0);
        RelNode adjustedNode = adjustRowType(rewrittenNode, originalNode.getRowType());

        call.transformTo(adjustedNode);
    }

    /**
     * Adjust the row type of the rewritten node to match the expected row type.
     * This is needed when the transformation changes nullability (e.g., LEFT JOIN -> INNER JOIN).
     *
     * @param node         the rewritten node
     * @param expectedType the expected row type from the original node
     * @return the adjusted node with matching row type, or the original node if types already match
     */
    private RelNode adjustRowType(RelNode node, RelDataType expectedType) {
        RelDataType actualType = node.getRowType();

        // If types are equal, no adjustment needed
        if (actualType.equals(expectedType)) {
            return node;
        }

        // Check if only nullability differs
        if (actualType.getFieldCount() != expectedType.getFieldCount()) {
            log.warn("Field count mismatch: expected {}, actual {}",
                expectedType.getFieldCount(), actualType.getFieldCount());
            return node;
        }

        // Create a Project with CAST expressions to adjust nullability
        RexBuilder rexBuilder = node.getCluster().getRexBuilder();
        RelDataTypeFactory typeFactory = node.getCluster().getTypeFactory();

        List<RexNode> castExprs = new ArrayList<>();
        List<String> fieldNames = new ArrayList<>();

        for (int i = 0; i < expectedType.getFieldCount(); i++) {
            RelDataTypeField expectedField = expectedType.getFieldList().get(i);
            RelDataTypeField actualField = actualType.getFieldList().get(i);

            RexNode inputRef = rexBuilder.makeInputRef(actualField.getType(), i);

            // If nullability differs, create a type with the expected nullability
            if (expectedField.getType().isNullable() != actualField.getType().isNullable()) {
                RelDataType targetType = typeFactory.createTypeWithNullability(
                    actualField.getType(), expectedField.getType().isNullable());
                castExprs.add(rexBuilder.makeCast(targetType, inputRef));
            } else {
                castExprs.add(inputRef);
            }

            fieldNames.add(expectedField.getName());
        }

        // Create a LogicalProject to adjust the types
        return LogicalProject.create(node, Collections.emptyList(), castExprs, fieldNames);
    }


    /**
     * Unwrap HepRelVertex to get the actual RelNode.
     * HepPlanner wraps RelNodes in HepRelVertex, we need to unwrap them for proper type checking.
     */
    private RelNode unwrapHepVertex(RelNode node) {
        if (node instanceof HepRelVertex) {
            return ((HepRelVertex) node).getCurrentRel();
        }
        return node;
    }

    private boolean recursiveMatch(RelNode template, RelNode query, Map<String, Object> bindings) {
        // Unwrap HepRelVertex if present
        query = unwrapHepVertex(query);

        log.info("recursiveMatch: template={}, query={}",
            template.getClass().getSimpleName(), query.getClass().getSimpleName());

        if (!template.getClass().equals(query.getClass())) {
            log.info("Class mismatch: {} != {}",
                template.getClass().getSimpleName(), query.getClass().getSimpleName());
            return false;
        }

        if (template instanceof LogicalTableScan) {
            return matchTableScan((LogicalTableScan) template, (LogicalTableScan) query, bindings);
        } else if (template instanceof LogicalProject) {
            return matchProject((LogicalProject) template, (LogicalProject) query, bindings);
        } else if (template instanceof LogicalFilter) {
            return matchFilter((LogicalFilter) template, (LogicalFilter) query, bindings);
        } else if (template instanceof LogicalJoin) {
            return matchJoin((LogicalJoin) template, (LogicalJoin) query, bindings);
        } else if (template instanceof LogicalAggregate) {
            return matchAggregate((LogicalAggregate) template, (LogicalAggregate) query, bindings);
        }

        if (template.getInputs().size() != query.getInputs().size()) {
            return false;
        }

        for (int i = 0; i < template.getInputs().size(); i++) {
            if (!recursiveMatch(template.getInput(i), query.getInput(i), bindings)) {
                return false;
            }
        }

        return true;
    }

    private boolean matchTableScan(LogicalTableScan template, LogicalTableScan query, Map<String, Object> bindings) {
        String templateTableName = template.getTable().getQualifiedName().get(0);

        if (templateTableName.matches("t\\d+")) {
            bindings.put(templateTableName, query);
            return true;
        }

        String queryTableName = query.getTable().getQualifiedName().get(0);
        return templateTableName.equals(queryTableName);
    }

    private boolean matchProject(LogicalProject template, LogicalProject query, Map<String, Object> bindings) {
        if (!recursiveMatch(template.getInput(), query.getInput(), bindings)) {
            return false;
        }

        List<RexNode> templateProjects = template.getProjects();
        List<RexNode> queryProjects = query.getProjects();

        if (templateProjects.size() != queryProjects.size()) {
            return false;
        }

        for (int i = 0; i < templateProjects.size(); i++) {
            if (!matchRexNode(templateProjects.get(i), queryProjects.get(i), bindings)) {
                return false;
            }
        }

        List<String> templateFields = template.getRowType().getFieldNames();
        List<String> queryFields = query.getRowType().getFieldNames();

        for (int i = 0; i < templateFields.size(); i++) {
            String templateField = templateFields.get(i);
            if (templateField.matches("a\\d+")) {
                bindings.put(templateField, queryFields.get(i));
            }
        }

        return true;
    }

    private boolean matchFilter(LogicalFilter template, LogicalFilter query, Map<String, Object> bindings) {
        log.info("matchFilter: template condition={}, query condition={}",
            template.getCondition(), query.getCondition());

        if (!recursiveMatch(template.getInput(), query.getInput(), bindings)) {
            log.info("matchFilter: input match failed");
            return false;
        }

        // Match the condition
        boolean result = matchRexNode(template.getCondition(), query.getCondition(), bindings);

        // Extract and bind attribute names from the filter condition
        // For example, Filter<p0 a0> should bind a0 to the field being filtered
        if (result && template.getCondition() instanceof RexCall) {
            extractAndBindAttributes(template.getCondition(), query.getCondition(), bindings);
        }

        return result;
    }

    /**
     * Extract and bind attribute names from RexNode expressions.
     * This is needed for constraints like AttrsEq(a0, a1) to work properly.
     */
    private void extractAndBindAttributes(RexNode template, RexNode query, Map<String, Object> bindings) {
        if (template instanceof RexInputRef && query instanceof RexInputRef) {
            RexInputRef templateRef = (RexInputRef) template;
            RexInputRef queryRef = (RexInputRef) query;

            String attrName = "a" + templateRef.getIndex();
            // Bind the attribute name to the field reference itself
            bindings.put(attrName, queryRef);
            log.debug("Bound attribute {} to field index {}", attrName, queryRef.getIndex());
        } else if (template instanceof RexCall && query instanceof RexCall) {
            RexCall templateCall = (RexCall) template;
            RexCall queryCall = (RexCall) query;

            // Recursively process operands
            for (int i = 0; i < templateCall.getOperands().size() && i < queryCall.getOperands().size(); i++) {
                extractAndBindAttributes(templateCall.getOperands().get(i), queryCall.getOperands().get(i), bindings);
            }
        }
    }

    private boolean matchJoin(LogicalJoin template, LogicalJoin query, Map<String, Object> bindings) {
        if (template.getJoinType() != query.getJoinType()) {
            return false;
        }

        if (!recursiveMatch(template.getLeft(), query.getLeft(), bindings)) {
            return false;
        }
        if (!recursiveMatch(template.getRight(), query.getRight(), bindings)) {
            return false;
        }

        // Match JOIN condition with special handling for attribute references
        return matchJoinCondition(template, query, bindings);
    }

    /**
     * Match JOIN condition with special handling.
     * Template uses attribute name indices (a0→0, a1→1), but query uses actual field indices.
     * We need to map between them.
     */
    private boolean matchJoinCondition(LogicalJoin template, LogicalJoin query, Map<String, Object> bindings) {
        RexNode templateCondition = template.getCondition();
        RexNode queryCondition = query.getCondition();

        // For now, use simple matching
        // TODO: Implement proper index mapping for JOIN conditions
        return matchRexNode(templateCondition, queryCondition, bindings);
    }

    private boolean matchAggregate(LogicalAggregate template, LogicalAggregate query, Map<String, Object> bindings) {
        if (!recursiveMatch(template.getInput(), query.getInput(), bindings)) {
            return false;
        }

        return template.getAggCallList().isEmpty() && query.getAggCallList().isEmpty();
    }

    private boolean matchRexNode(RexNode template, RexNode query, Map<String, Object> bindings) {
        if (template instanceof RexInputRef && query instanceof RexInputRef) {
            RexInputRef templateRef = (RexInputRef) template;
            RexInputRef queryRef = (RexInputRef) query;

            // Template uses attribute name indices (a0→0, a1→1)
            // Query uses actual field indices from its RelNode
            // For now, we'll just check if they reference fields at the same position
            // TODO: Implement proper mapping considering parent RelNode structure

            // Store the binding for later use
            String attrName = "a" + templateRef.getIndex();
            bindings.put(attrName + "_index", queryRef.getIndex());

            return true; // Allow flexible matching for now
        }

        if (template instanceof RexCall && query instanceof RexCall) {
            RexCall templateCall = (RexCall) template;
            RexCall queryCall = (RexCall) query;

            String operatorName = templateCall.getOperator().getName();
            if (operatorName.matches("p\\d+")) {
                bindings.put(operatorName, queryCall);
                return true;
            }

            if (!templateCall.getOperator().equals(queryCall.getOperator())) {
                return false;
            }

            if (templateCall.getOperands().size() != queryCall.getOperands().size()) {
                return false;
            }

            for (int i = 0; i < templateCall.getOperands().size(); i++) {
                if (!matchRexNode(templateCall.getOperands().get(i), queryCall.getOperands().get(i), bindings)) {
                    return false;
                }
            }

            return true;
        }

        return template.equals(query);
    }

    private boolean checkMatchConstraints(Map<String, Object> bindings) {
        if (matchConstraints == null || matchConstraints.isEmpty()) {
            return true;
        }

        log.debug("Checking {} match constraints with bindings: {}", matchConstraints.size(), bindings.keySet());

        for (ASTNode constraint : matchConstraints) {
            if (constraint instanceof org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment) {
                org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment cs =
                    (org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment) constraint;

                String constraintType = cs.getType().name();
                String[] params = cs.getParams();

                if (params == null || params.length < 2) {
                    continue;
                }

                boolean satisfied = evaluateMatchConstraint(constraintType, params, bindings);
                if (!satisfied) {
                    log.debug("Constraint {} failed with params: {}", constraintType, Arrays.toString(params));
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Evaluate a single match constraint.
     */
    private boolean evaluateMatchConstraint(String constraintType, String[] params, Map<String, Object> bindings) {
        switch (constraintType) {
            case "PREDICATE_EQ":
                return evaluatePredicateEq(params[0], params[1], bindings);
            case "ATTRS_EQ":
                return evaluateAttrsEq(params[0], params[1], bindings);
            case "ATTRS_SUB":
                return bindings.containsKey(params[0]) || bindings.containsKey(params[0] + "_index");
            case "TABLE_EQ":
                return evaluateTableEq(params[0], params[1], bindings);
            case "UNIQUE":
            case "NOT_NULL":
            case "REFERENCE":
                // These constraints require metadata - for now, assume true
                // TODO: Implement metadata-based constraint checking
                return true;
            default:
                log.debug("Unknown constraint type: {}", constraintType);
                return true;
        }
    }

    /**
     * Check if two predicates are equivalent.
     */
    private boolean evaluatePredicateEq(String p1, String p2, Map<String, Object> bindings) {
        Object pred1 = bindings.get(p1);
        Object pred2 = bindings.get(p2);

        if (pred1 == null || pred2 == null) {
            log.debug("PredicateEq: {} or {} not bound", p1, p2);
            return false;
        }

        if (pred1 instanceof RexNode && pred2 instanceof RexNode) {
            // Compare RexNode string representations for equality
            String str1 = pred1.toString();
            String str2 = pred2.toString();
            boolean eq = str1.equals(str2);
            log.debug("PredicateEq({}, {}): {} vs {} = {}", p1, p2, str1, str2, eq);
            return eq;
        }

        return pred1.equals(pred2);
    }

    /**
     * Check if two attributes are equivalent.
     */
    private boolean evaluateAttrsEq(String a1, String a2, Map<String, Object> bindings) {
        // Check indexed bindings first
        Object idx1 = bindings.get(a1 + "_index");
        Object idx2 = bindings.get(a2 + "_index");

        if (idx1 != null && idx2 != null) {
            boolean eq = idx1.equals(idx2);
            log.debug("AttrsEq({}, {}): index {} vs {} = {}", a1, a2, idx1, idx2, eq);
            return eq;
        }

        // Check direct bindings (RexInputRef objects)
        Object val1 = bindings.get(a1);
        Object val2 = bindings.get(a2);

        if (val1 != null && val2 != null) {
            // If both are RexInputRef, compare their indices
            if (val1 instanceof RexInputRef && val2 instanceof RexInputRef) {
                RexInputRef ref1 = (RexInputRef) val1;
                RexInputRef ref2 = (RexInputRef) val2;
                boolean eq = ref1.getIndex() == ref2.getIndex();
                log.debug("AttrsEq({}, {}): RexInputRef {} vs {} = {}", a1, a2, ref1.getIndex(), ref2.getIndex(), eq);
                return eq;
            }

            // For other types, use equals
            boolean eq = val1.equals(val2);
            log.debug("AttrsEq({}, {}): {} vs {} = {}", a1, a2, val1, val2, eq);
            return eq;
        }

        // If one is bound and one is not, they can't be equal
        log.debug("AttrsEq({}, {}): one or both not bound (val1={}, val2={})", a1, a2, val1, val2);
        return false;
    }

    /**
     * Check if two tables are the same.
     */
    private boolean evaluateTableEq(String t1, String t2, Map<String, Object> bindings) {
        Object table1 = bindings.get(t1);
        Object table2 = bindings.get(t2);

        if (table1 == null || table2 == null) {
            return false;
        }

        if (table1 instanceof LogicalTableScan && table2 instanceof LogicalTableScan) {
            String name1 = ((LogicalTableScan) table1).getTable().getQualifiedName().toString();
            String name2 = ((LogicalTableScan) table2).getTable().getQualifiedName().toString();
            return name1.equals(name2);
        }

        return table1.equals(table2);
    }

    private Map<String, Object> applyRewriteConstraints(Map<String, Object> sourceBindings) {
        Map<String, Object> targetBindings = new HashMap<>(sourceBindings);

        if (rewriteConstraints == null || rewriteConstraints.isEmpty()) {
            return targetBindings;
        }

        log.debug("Applying {} rewrite constraints", rewriteConstraints.size());

        // Parse and apply rewrite constraints
        // Rewrite constraints map target placeholders to source placeholders
        // Examples: TableEq(t2, t0), AttrsEq(a3, a0), SchemaEq(s1, s0), PredicateEq(p1, p0)
        // Format: ConstraintName(targetParam, sourceParam)
        for (ASTNode constraint : rewriteConstraints) {
            if (constraint instanceof org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment) {
                org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment cs =
                    (org.apache.shardingsphere.sql.parser.statement.core.segment.rewriter.ConstraintSegment) constraint;

                String constraintType = cs.getType().name();
                String[] params = cs.getParams();

                if (params == null || params.length < 2) {
                    continue;
                }

                String targetParam = params[0]; // Target template parameter (t2, a3, s1, p1)
                String sourceParam = params[1]; // Source template parameter (t0, a0, s0, p0)

                // Map target to source value
                if (sourceBindings.containsKey(sourceParam)) {
                    Object value = sourceBindings.get(sourceParam);
                    targetBindings.put(targetParam, value);
                    log.debug("Constraint {}({}, {}) -> {}", constraintType, targetParam, sourceParam,
                        value instanceof RelNode ? ((RelNode)value).getClass().getSimpleName() : value);
                }

                // Also handle indexed bindings (for RexInputRef)
                String sourceIndexKey = sourceParam + "_index";
                if (sourceBindings.containsKey(sourceIndexKey)) {
                    Object indexValue = sourceBindings.get(sourceIndexKey);
                    targetBindings.put(targetParam + "_index", indexValue);
                }
            }
        }

        return targetBindings;
    }

    private RelNode fillTargetTemplate(RelNode template, Map<String, Object> bindings) {
        if (template instanceof LogicalTableScan) {
            return fillTableScan((LogicalTableScan) template, bindings);
        } else if (template instanceof LogicalProject) {
            return fillProject((LogicalProject) template, bindings);
        } else if (template instanceof LogicalFilter) {
            return fillFilter((LogicalFilter) template, bindings);
        } else if (template instanceof LogicalJoin) {
            return fillJoin((LogicalJoin) template, bindings);
        } else if (template instanceof LogicalAggregate) {
            return fillAggregate((LogicalAggregate) template, bindings);
        }

        return template;
    }

    private RelNode fillTableScan(LogicalTableScan template, Map<String, Object> bindings) {
        String templateTableName = template.getTable().getQualifiedName().get(0);

        if (templateTableName.matches("t\\d+") && bindings.containsKey(templateTableName)) {
            Object boundValue = bindings.get(templateTableName);

            // The bound value should be a LogicalTableScan from the source query
            if (boundValue instanceof LogicalTableScan) {
                return (LogicalTableScan) boundValue;
            }

            // If it's a RelNode but not a TableScan, we need to extract the TableScan
            // This can happen if the binding contains a more complex node
            if (boundValue instanceof RelNode) {
                RelNode boundNode = (RelNode) boundValue;
                // Try to find the TableScan in the tree
                LogicalTableScan tableScan = findTableScan(boundNode);
                if (tableScan != null) {
                    return tableScan;
                }
            }
        }

        return template;
    }

    /**
     * Find a LogicalTableScan in a RelNode tree.
     * This is a simple depth-first search.
     */
    private LogicalTableScan findTableScan(RelNode node) {
        if (node instanceof LogicalTableScan) {
            return (LogicalTableScan) node;
        }

        for (RelNode input : node.getInputs()) {
            LogicalTableScan found = findTableScan(input);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private RelNode fillProject(LogicalProject template, Map<String, Object> bindings) {
        RelNode filledInput = fillTargetTemplate(template.getInput(), bindings);

        List<RexNode> filledProjects = new ArrayList<>();
        for (RexNode project : template.getProjects()) {
            filledProjects.add(fillRexNode(project, bindings, filledInput));
        }

        List<String> filledFieldNames = new ArrayList<>();
        for (String fieldName : template.getRowType().getFieldNames()) {
            if (fieldName.matches("a\\d+") && bindings.containsKey(fieldName)) {
                Object boundValue = bindings.get(fieldName);
                if (boundValue instanceof String) {
                    filledFieldNames.add((String) boundValue);
                } else {
                    filledFieldNames.add(fieldName);
                }
            } else {
                filledFieldNames.add(fieldName);
            }
        }

        return LogicalProject.create(filledInput, Collections.emptyList(), filledProjects, filledFieldNames);
    }

    private RelNode fillFilter(LogicalFilter template, Map<String, Object> bindings) {
        RelNode filledInput = fillTargetTemplate(template.getInput(), bindings);
        RexNode filledCondition = fillRexNode(template.getCondition(), bindings, filledInput);

        return LogicalFilter.create(filledInput, filledCondition);
    }

    private RelNode fillJoin(LogicalJoin template, Map<String, Object> bindings) {
        RelNode filledLeft = fillTargetTemplate(template.getLeft(), bindings);
        RelNode filledRight = fillTargetTemplate(template.getRight(), bindings);

        // For JOIN condition, we need to handle field references that span both sides
        // Build a virtual JOIN node to get the correct combined row type
        RexNode filledCondition = fillJoinCondition(
            template.getCondition(),
            bindings,
            filledLeft,
            filledRight
        );

        return LogicalJoin.create(
            filledLeft,
            filledRight,
            template.getHints(),
            filledCondition,
            template.getVariablesSet(),
            template.getJoinType()
        );
    }

    private RelNode fillAggregate(LogicalAggregate template, Map<String, Object> bindings) {
        RelNode filledInput = fillTargetTemplate(template.getInput(), bindings);

        return LogicalAggregate.create(
            filledInput,
            template.getHints(),
            template.getGroupSet(),
            template.getGroupSets(),
            template.getAggCallList()
        );
    }

    /**
     * Fill JOIN condition with special handling for field references.
     * JOIN conditions reference fields from both left and right inputs.
     * Field indices need to be adjusted based on which side they come from.
     */
    private RexNode fillJoinCondition(RexNode template, Map<String, Object> bindings,
                                      RelNode leftInput, RelNode rightInput) {
        if (template instanceof RexInputRef) {
            RexInputRef templateRef = (RexInputRef) template;
            int templateIndex = templateRef.getIndex();
            String attrName = "a" + templateIndex;

            // Check if we have a mapped index from the source query
            String indexKey = attrName + "_index";
            if (bindings.containsKey(indexKey)) {
                int actualIndex = (Integer) bindings.get(indexKey);

                // Determine which side this field comes from
                int leftFieldCount = leftInput.getRowType().getFieldCount();

                if (actualIndex < leftFieldCount) {
                    // Field from left side
                    return leftInput.getCluster().getRexBuilder().makeInputRef(
                        leftInput.getRowType().getFieldList().get(actualIndex).getType(),
                        actualIndex
                    );
                } else {
                    // Field from right side - adjust index
                    int rightIndex = actualIndex - leftFieldCount;
                    return rightInput.getCluster().getRexBuilder().makeInputRef(
                        rightInput.getRowType().getFieldList().get(rightIndex).getType(),
                        actualIndex  // Keep the combined index for JOIN context
                    );
                }
            }

            // Fallback: use template index
            int leftFieldCount = leftInput.getRowType().getFieldCount();
            if (templateIndex < leftFieldCount) {
                return leftInput.getCluster().getRexBuilder().makeInputRef(
                    leftInput.getRowType().getFieldList().get(templateIndex).getType(),
                    templateIndex
                );
            } else {
                int rightIndex = templateIndex - leftFieldCount;
                if (rightIndex < rightInput.getRowType().getFieldCount()) {
                    return rightInput.getCluster().getRexBuilder().makeInputRef(
                        rightInput.getRowType().getFieldList().get(rightIndex).getType(),
                        templateIndex
                    );
                }
            }

            return template;
        }

        if (template instanceof RexCall) {
            RexCall templateCall = (RexCall) template;
            String operatorName = templateCall.getOperator().getName();

            if (operatorName.matches("p\\d+") && bindings.containsKey(operatorName)) {
                return (RexNode) bindings.get(operatorName);
            }

            List<RexNode> filledOperands = new ArrayList<>();
            for (RexNode operand : templateCall.getOperands()) {
                filledOperands.add(fillJoinCondition(operand, bindings, leftInput, rightInput));
            }

            return templateCall.clone(templateCall.getType(), filledOperands);
        }

        return template;
    }

    private RexNode fillRexNode(RexNode template, Map<String, Object> bindings, RelNode input) {
        if (template instanceof RexInputRef) {
            RexInputRef templateRef = (RexInputRef) template;
            int templateIndex = templateRef.getIndex();
            String attrName = "a" + templateIndex;

            // Check if we have a mapped index from the source query
            String indexKey = attrName + "_index";
            if (bindings.containsKey(indexKey)) {
                int actualIndex = (Integer) bindings.get(indexKey);
                // Create RexInputRef with the actual field type from input
                return input.getCluster().getRexBuilder().makeInputRef(
                    input.getRowType().getFieldList().get(actualIndex).getType(),
                    actualIndex
                );
            }

            // Fallback: use template index if within bounds
            if (templateIndex < input.getRowType().getFieldCount()) {
                return input.getCluster().getRexBuilder().makeInputRef(
                    input.getRowType().getFieldList().get(templateIndex).getType(),
                    templateIndex
                );
            }

            // If no mapping found, return as-is (might cause error)
            return template;
        }

        if (template instanceof RexCall) {
            RexCall templateCall = (RexCall) template;
            String operatorName = templateCall.getOperator().getName();

            if (operatorName.matches("p\\d+") && bindings.containsKey(operatorName)) {
                return (RexNode) bindings.get(operatorName);
            }

            List<RexNode> filledOperands = new ArrayList<>();
            for (RexNode operand : templateCall.getOperands()) {
                filledOperands.add(fillRexNode(operand, bindings, input));
            }

            return templateCall.clone(templateCall.getType(), filledOperands);
        }

        return template;
    }
}
