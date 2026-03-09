package org.autorewriter.rewriter.rule;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.autorewriter.rewriter.analyze.RuleAnalysisContext;
import org.autorewriter.rewriter.rule.constraint.Constraints;
import org.autorewriter.rewriter.rule.instantiation.Instantiation;
import org.autorewriter.rewriter.rule.match.Match;
import org.autorewriter.rewriter.rule.model.Model;
import org.autorewriter.rewriter.rule.symbol.Symbol;
import org.autorewriter.rewriter.rule.symbol.SymbolExtractor;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.autorewriter.rewriter.rule.util.ColumnRefRegistry;

import java.util.*;

/**
 * Auto rewrite rule that matches source template RelNode and rewrites to target template RelNode.
 * Uses WeTune-style Symbol/Model/Match/Instantiation pipeline.
 */
@Slf4j
public class AutoRewriteRule extends RelOptRule {

    private final int ruleId;
    private final RelNode sourceTemplate;
    private final RelNode targetTemplate;
    private final Constraints constraints;

    // Cache the Model from the last successful matches() call.
    // In VolcanoPlanner, RelSubset contents may change between matches() and onMatch(),
    // causing re-matching in onMatch() to fail. Caching avoids this race condition.
    private volatile Model lastMatchedModel;
    private volatile RelNode lastMatchedNode;

    public AutoRewriteRule(RelOptRuleOperand operand, RuleAnalysisContext ruleContext) {
        this(operand, ruleContext, -1);
    }

    public AutoRewriteRule(RelOptRuleOperand operand, RuleAnalysisContext ruleContext, int ruleId) {
        this(operand, ruleContext, ruleId, "");
    }

    public AutoRewriteRule(RelOptRuleOperand operand, RuleAnalysisContext ruleContext,
                           int ruleId, String descSuffix) {
        super(operand, "AutoRewriteRule_"
                + (ruleId >= 0 ? ruleId : System.identityHashCode(ruleContext))
                + descSuffix);
        this.ruleId = ruleId;
        this.sourceTemplate = ruleContext.getSourceRelNode();
        this.targetTemplate = ruleContext.getTargetRelNode();

        Map<String, Symbol> sourceSymbols = SymbolExtractor.extract(sourceTemplate);
        Map<String, Symbol> targetSymbols = SymbolExtractor.extract(targetTemplate);
        this.constraints = Constraints.build(
                ruleContext.getMatchConstraints(),
                ruleContext.getRewriteConstraints(),
                sourceSymbols, targetSymbols);
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
        RelNode queryNode = call.rel(0);

        // VolcanoPlanner's IterativeRuleDriver re-checks matches() via assert
        // before calling onMatch(). Since our deep structural matching traverses
        // RelSubsets whose contents may change between calls, the re-check could
        // fail. Return cached result for the same node to satisfy the assertion.
        if (queryNode == lastMatchedNode && lastMatchedModel != null) {
            return true;
        }

        // Create a fresh Model for this match attempt
        Model model = new Model(constraints);

        if (!Match.match(sourceTemplate, queryNode, model)) {
            log.info("Rule[{}] match failed: structure does not match", ruleId);
            return false;
        }

        // Check all constraints (ATTRS_SUB, UNIQUE, NOT_NULL, REFERENCE)
        if (!model.checkConstraints()) {
            log.info("Rule[{}] match failed: constraints not satisfied", ruleId);
            return false;
        }

        log.info("Rule[{}] match succeeded", ruleId);

        // Cache for onMatch() and assertion re-check
        this.lastMatchedModel = model;
        this.lastMatchedNode = queryNode;

        return true;
    }

    @Override
    public void onMatch(RelOptRuleCall call) {
        log.debug("Rule[{}] matched, applying rewrite", ruleId);

        try {
            RelNode queryNode = call.rel(0);

            // Reuse cached Model if available (same node identity),
            // otherwise fall back to re-matching
            Model model;
            if (queryNode == lastMatchedNode && lastMatchedModel != null) {
                model = lastMatchedModel;
            } else {
                model = new Model(constraints);
                if (!Match.match(sourceTemplate, queryNode, model)) {
                    log.warn("Rule[{}] onMatch re-match failed unexpectedly", ruleId);
                    return;
                }
                if (!model.checkConstraints()) {
                    log.warn("Rule[{}] onMatch re-match constraints failed", ruleId);
                    return;
                }
            }
            // Clear cache
            lastMatchedModel = null;
            lastMatchedNode = null;

            RelNode rewrittenNode = Instantiation.instantiate(targetTemplate, model, constraints);

            RelNode originalNode = call.rel(0);
            RelNode adjustedNode = adjustRowType(rewrittenNode, originalNode.getRowType());

            // Verify row type compatibility before transforming — Calcite's HepPlanner
            // will throw if the field counts don't match
            if (adjustedNode.getRowType().getFieldCount() != originalNode.getRowType().getFieldCount()) {
                log.warn("Rule[{}] rewrite aborted: field count mismatch (expected {}, got {})",
                        ruleId, originalNode.getRowType().getFieldCount(),
                        adjustedNode.getRowType().getFieldCount());
                return;
            }

            // Verify output column identity preservation (WeTune-aligned).
            // The rewritten plan must produce the same output columns as the original.
            // This prevents over-matching where a stripped rule matches the wrong node
            // (e.g., outer InSubFilter instead of inner) and corrupts the plan.
            ColumnRefRegistry verifyRegistry = new ColumnRefRegistry();
            List<ColumnRef> origCols = verifyRegistry.outputColumnsOf(originalNode);
            List<ColumnRef> newCols = verifyRegistry.outputColumnsOf(adjustedNode);
            if (!origCols.equals(newCols)) {
                log.info("Rule[{}] rewrite aborted: output columns changed {} -> {}",
                        ruleId, origCols, newCols);
                return;
            }

            call.transformTo(adjustedNode);
        } catch (Exception e) {
            log.warn("Rule[{}] rewrite failed: {}", ruleId, e.getMessage(), e);
        }
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
}
