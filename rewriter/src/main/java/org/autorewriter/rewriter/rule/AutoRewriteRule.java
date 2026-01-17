package org.autorewriter.rewriter.rule;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.*;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexCall;
import org.apache.shardingsphere.sql.parser.api.ASTNode;
import org.autorewriter.rewriter.analyze.RuleAnalysisContext;

import java.util.*;

/**
 * Auto rewrite rule that matches source template RelNode and rewrites to target template RelNode.
 */
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

        // Directly transform without type adjustment
        // Rule-based rewriting should preserve semantic equivalence
        // Type changes (e.g., nullability from LEFT JOIN -> INNER JOIN) are correct
        call.transformTo(rewrittenNode);
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

        if (!template.getClass().equals(query.getClass())) {
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
        if (!recursiveMatch(template.getInput(), query.getInput(), bindings)) {
            return false;
        }

        return matchRexNode(template.getCondition(), query.getCondition(), bindings);
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

        // TODO: Implement constraint evaluation
        return true;
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
