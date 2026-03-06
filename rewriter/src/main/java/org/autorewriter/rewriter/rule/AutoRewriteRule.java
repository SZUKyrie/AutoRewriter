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
import org.apache.shardingsphere.sql.parser.api.ASTNode;
import org.autorewriter.rewriter.analyze.RuleAnalysisContext;
import org.autorewriter.rewriter.rule.constraint.ConstraintEvaluator;
import org.autorewriter.rewriter.rule.filler.*;
import org.autorewriter.rewriter.rule.matcher.*;
import org.autorewriter.rewriter.rule.util.RexNodeFiller;
import org.autorewriter.rewriter.rule.util.RexNodeMatcher;

import java.util.*;

/**
 * Auto rewrite rule that matches source template RelNode and rewrites to target template RelNode.
 */
@Slf4j
public class AutoRewriteRule extends RelOptRule {

    private final int ruleId;
    private final RelNode sourceTemplate;
    private final RelNode targetTemplate;
    private final List<ASTNode> matchConstraints;
    private final List<ASTNode> rewriteConstraints;
    private final Map<String, Object> placeholderBindings;

    private final TableScanMatcher tableScanMatcher;
    private final ProjectMatcher projectMatcher;
    private final FilterMatcher filterMatcher;
    private final JoinMatcher joinMatcher;
    private final AggregateMatcher aggregateMatcher;

    private final TableScanFiller tableScanFiller;
    private final ProjectFiller projectFiller;
    private final FilterFiller filterFiller;
    private final JoinFiller joinFiller;
    private final AggregateFiller aggregateFiller;

    private final ConstraintEvaluator constraintEvaluator;

    public AutoRewriteRule(RelOptRuleOperand operand, RuleAnalysisContext ruleContext) {
        this(operand, ruleContext, -1);
    }

    public AutoRewriteRule(RelOptRuleOperand operand, RuleAnalysisContext ruleContext, int ruleId) {
        super(operand);
        this.ruleId = ruleId;
        this.sourceTemplate = ruleContext.getSourceRelNode();
        this.targetTemplate = ruleContext.getTargetRelNode();
        this.matchConstraints = ruleContext.getMatchConstraints();
        this.rewriteConstraints = ruleContext.getRewriteConstraints();
        this.placeholderBindings = new HashMap<>();

        RexNodeMatcher rexNodeMatcher = new RexNodeMatcher(this::recursiveMatchInternal);
        RexNodeFiller rexNodeFiller = new RexNodeFiller(this::fillTargetTemplate);
        this.constraintEvaluator = new ConstraintEvaluator();

        this.tableScanMatcher = new TableScanMatcher();
        this.projectMatcher = new ProjectMatcher(this::recursiveMatchInternal, rexNodeMatcher);
        this.filterMatcher = new FilterMatcher(this::recursiveMatchInternal, rexNodeMatcher);
        this.joinMatcher = new JoinMatcher(this::recursiveMatchInternal, rexNodeMatcher);
        this.aggregateMatcher = new AggregateMatcher(this::recursiveMatchInternal);

        this.tableScanFiller = new TableScanFiller();
        this.projectFiller = new ProjectFiller(this::fillTargetTemplate, rexNodeFiller);
        this.filterFiller = new FilterFiller(this::fillTargetTemplate, rexNodeFiller);
        this.joinFiller = new JoinFiller(this::fillTargetTemplate, rexNodeFiller);
        this.aggregateFiller = new AggregateFiller(this::fillTargetTemplate);
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
        log.debug("Trying to match rule[{}] on node: {}", ruleId, call.rel(0).getClass().getSimpleName());
        RelNode queryNode = call.rel(0);
        placeholderBindings.clear();

        if (!recursiveMatch(sourceTemplate, queryNode, placeholderBindings)) {
            log.info("Rule[{}] match failed: structure does not match", ruleId);
            return false;
        }

        boolean res = constraintEvaluator.checkMatchConstraints(matchConstraints, placeholderBindings);
        if(!res) {
            log.info("Rule[{}] match failed: constraints not satisfied", ruleId);
        } else {
            log.info("Rule[{}] match succeeded", ruleId);
        }
        return res;
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
        log.debug("Rule matched, bindings: {}", placeholderBindings.keySet());
        Map<String, Object> rewriteBindings = constraintEvaluator.applyRewriteConstraints(rewriteConstraints, placeholderBindings);
        log.debug("Rewrite bindings: {}", rewriteBindings.keySet());

        RelNode rewrittenNode = fillTargetTemplate(targetTemplate, rewriteBindings);

        RelNode originalNode = call.rel(0);
        RelNode adjustedNode = adjustRowType(rewrittenNode, originalNode.getRowType());

        call.transformTo(adjustedNode);
    }

    private RelNode adjustRowType(RelNode node, RelDataType expectedType) {
        RelDataType actualType = node.getRowType();

        if (actualType.equals(expectedType)) {
            return node;
        }

        if (actualType.getFieldCount() != expectedType.getFieldCount()) {
            log.warn("Field count mismatch: expected {}, actual {}",
                expectedType.getFieldCount(), actualType.getFieldCount());
            return node;
        }

        // Check if types are compatible (same base type, only nullability differs)
        boolean onlyNullabilityDiffers = true;
        for (int i = 0; i < expectedType.getFieldCount(); i++) {
            RelDataTypeField expectedField = expectedType.getFieldList().get(i);
            RelDataTypeField actualField = actualType.getFieldList().get(i);

            // Get base types without nullability
            RelDataType expectedBase = expectedField.getType();
            RelDataType actualBase = actualField.getType();

            // Check if base types are the same (ignoring nullability)
            if (expectedBase.getSqlTypeName() != actualBase.getSqlTypeName()) {
                onlyNullabilityDiffers = false;
                log.warn("Type mismatch at field {}: expected {}, actual {}",
                    i, expectedBase.getSqlTypeName(), actualBase.getSqlTypeName());
                break;
            }
        }

        // Only adjust if types are compatible
        if (!onlyNullabilityDiffers) {
            log.warn("Cannot adjust row type: types are incompatible");
            log.warn("Expected: {}", expectedType);
            log.warn("Actual: {}", actualType);
            return node;
        }

        RexBuilder rexBuilder = node.getCluster().getRexBuilder();
        RelDataTypeFactory typeFactory = node.getCluster().getTypeFactory();

        List<RexNode> castExprs = new ArrayList<>();
        List<String> fieldNames = new ArrayList<>();

        for (int i = 0; i < expectedType.getFieldCount(); i++) {
            RelDataTypeField expectedField = expectedType.getFieldList().get(i);
            RelDataTypeField actualField = actualType.getFieldList().get(i);

            RexNode inputRef = rexBuilder.makeInputRef(actualField.getType(), i);

            if (expectedField.getType().isNullable() != actualField.getType().isNullable()) {
                RelDataType targetType = typeFactory.createTypeWithNullability(
                    actualField.getType(), expectedField.getType().isNullable());
                castExprs.add(rexBuilder.makeCast(targetType, inputRef));
            } else {
                castExprs.add(inputRef);
            }

            fieldNames.add(expectedField.getName());
        }

        return LogicalProject.create(node, Collections.emptyList(), castExprs, fieldNames);
    }

    private RelNode unwrapHepVertex(RelNode node) {
        if (node instanceof HepRelVertex) {
            return ((HepRelVertex) node).getCurrentRel();
        }
        return node;
    }

    private boolean recursiveMatch(RelNode template, RelNode query, Map<String, Object> bindings) {
        query = unwrapHepVertex(query);

//        log.info("recursiveMatch: template={}, query={}",
//            template.getClass().getSimpleName(), query.getClass().getSimpleName());

        if (template instanceof LogicalTableScan) {
            return tableScanMatcher.matchInputPlaceholder((LogicalTableScan) template, query, bindings);
        }

        if (!template.getClass().equals(query.getClass())) {
//            log.info("Class mismatch: {} != {}",
//                template.getClass().getSimpleName(), query.getClass().getSimpleName());
            return false;
        }

        if (template instanceof LogicalProject) {
            return projectMatcher.match((LogicalProject) template, (LogicalProject) query, bindings);
        } else if (template instanceof LogicalFilter) {
            return filterMatcher.match((LogicalFilter) template, (LogicalFilter) query, bindings);
        } else if (template instanceof LogicalJoin) {
            return joinMatcher.match((LogicalJoin) template, (LogicalJoin) query, bindings);
        } else if (template instanceof LogicalAggregate) {
            return aggregateMatcher.match((LogicalAggregate) template, (LogicalAggregate) query, bindings);
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

    private Boolean recursiveMatchInternal(RelNode template, RelNode query) {
        return recursiveMatch(template, query, placeholderBindings);
    }

    private RelNode fillTargetTemplate(RelNode template, Map<String, Object> bindings) {
        if (template instanceof LogicalTableScan) {
            return tableScanFiller.fill((LogicalTableScan) template, bindings);
        } else if (template instanceof LogicalProject) {
            return projectFiller.fill((LogicalProject) template, bindings);
        } else if (template instanceof LogicalFilter) {
            return filterFiller.fill((LogicalFilter) template, bindings);
        } else if (template instanceof LogicalJoin) {
            return joinFiller.fill((LogicalJoin) template, bindings);
        } else if (template instanceof LogicalAggregate) {
            return aggregateFiller.fill((LogicalAggregate) template, bindings);
        }

        return template;
    }

    private RelNode fillTargetTemplate(RelNode template) {
        return fillTargetTemplate(template, placeholderBindings);
    }
}
