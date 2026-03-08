package org.autorewriter.rewriter.rule.instantiation;

import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.logical.LogicalJoin;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.autorewriter.rewriter.rule.util.ColumnRefRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Normalizes join trees to left-deep form by rotating right-deep patterns.
 * Aligned with WeTune's {@code NormalizeJoin} which runs after each rule
 * instantiation to ensure consistent join tree shape.
 *
 * <p>The rotation transforms:
 * <pre>
 *   join0(A, join1(B, C))  =>  join1(join0(A, B), C)   [Pattern 1: keys in B]
 *   join0(A, join1(B, C))  =>  join1(join0(A, C), B)   [Pattern 2: keys in C]
 * </pre>
 *
 * <p>This is applied recursively until the entire join tree is left-deep,
 * meaning no join has another join as its right child.
 */
public class NormalizeJoin {

    private final ColumnRefRegistry registry;

    public NormalizeJoin() {
        this.registry = new ColumnRefRegistry();
    }

    /**
     * Normalize the join tree rooted at {@code root} to left-deep form.
     * Processes the entire tree, normalizing join subtrees found anywhere.
     */
    public RelNode normalize(RelNode root) {
        return normalizeTree(unwrap(root));
    }

    private RelNode normalizeTree(RelNode node) {
        node = unwrap(node);

        // Recursively normalize children first (bottom-up)
        List<RelNode> newInputs = new ArrayList<>();
        boolean changed = false;
        for (RelNode input : node.getInputs()) {
            RelNode normalized = normalizeTree(input);
            newInputs.add(normalized);
            if (normalized != input) changed = true;
        }
        if (changed) {
            node = node.copy(node.getTraitSet(), newInputs);
        }

        // Only normalize Join nodes
        if (!(node instanceof Join)) {
            return node;
        }

        return normalizeJoin((Join) node);
    }

    /**
     * If the right child of this join is also a join, rotate to left-deep.
     * Applied recursively until no right child is a join.
     */
    private RelNode normalizeJoin(Join join) {
        RelNode lhs = unwrap(join.getLeft());
        RelNode rhs = unwrap(join.getRight());

        // Look through identity Projections to find underlying Joins
        RelNode rhsInner = unwrapIdentityProject(rhs);

        // Only rotate if RHS (possibly under an identity Proj) is a join
        if (!(rhsInner instanceof Join)) {
            return join;
        }

        Join rhsJoin = (Join) rhsInner;
        RelNode b = unwrap(rhsJoin.getLeft());
        RelNode c = unwrap(rhsJoin.getRight());

        // Determine which side of rhsJoin contains the keys referenced
        // by join's right-side join condition
        List<ColumnRef> rhsJoinKeys = extractRhsJoinKeys(join);
        List<ColumnRef> bCols = registry.outputColumnsOf(b);

        boolean keysInB = false;
        for (ColumnRef key : rhsJoinKeys) {
            if (bCols.contains(key)) {
                keysInB = true;
                break;
            }
        }

        RelNode result;
        if (keysInB) {
            // Pattern 1: join0(A, join1(B, C)) => join1(join0(A, B), C)
            RelNode newInner = rebuildJoin(join, lhs, b);
            // Recursively normalize newInner: after rotation, it may still be
            // right-deep (e.g., B was itself a multi-way join from a query binding)
            if (newInner instanceof Join) {
                newInner = normalizeJoin((Join) newInner);
            }
            result = rebuildJoin(rhsJoin, newInner, c);
        } else {
            // Pattern 2: join0(A, join1(B, C)) => join1(join0(A, C), B)
            RelNode newInner = rebuildJoin(join, lhs, c);
            if (newInner instanceof Join) {
                newInner = normalizeJoin((Join) newInner);
            }
            result = rebuildJoin(rhsJoin, newInner, b);
        }

        // Recurse: the result might still have right-deep patterns
        if (result instanceof Join) {
            result = normalizeJoin((Join) result);
        }

        return result;
    }

    /**
     * Extract the ColumnRefs referenced by the join's right-side key
     * (the RexInputRef with index >= leftFieldCount).
     */
    private List<ColumnRef> extractRhsJoinKeys(Join join) {
        List<ColumnRef> result = new ArrayList<>();
        RexNode condition = join.getCondition();
        int leftFieldCount = join.getLeft().getRowType().getFieldCount();
        List<ColumnRef> allCols = registry.outputColumnsOf(join);

        extractRhsKeysFromCondition(condition, leftFieldCount, allCols, result);
        return result;
    }

    private void extractRhsKeysFromCondition(RexNode condition, int leftFieldCount,
                                              List<ColumnRef> allCols, List<ColumnRef> result) {
        if (condition instanceof org.apache.calcite.rex.RexCall) {
            org.apache.calcite.rex.RexCall call = (org.apache.calcite.rex.RexCall) condition;
            String opName = call.getOperator().getName();
            if ("=".equals(opName) && call.getOperands().size() == 2) {
                for (RexNode operand : call.getOperands()) {
                    if (operand instanceof RexInputRef) {
                        int idx = ((RexInputRef) operand).getIndex();
                        if (idx >= leftFieldCount && idx < allCols.size()) {
                            result.add(allCols.get(idx));
                        }
                    }
                }
            } else if ("AND".equals(opName)) {
                for (RexNode operand : call.getOperands()) {
                    extractRhsKeysFromCondition(operand, leftFieldCount, allCols, result);
                }
            }
        }
    }

    /**
     * Rebuild a join with new left and right children, adjusting the
     * join condition's RexInputRef indices using ColumnRefRegistry.
     */
    private RelNode rebuildJoin(Join originalJoin, RelNode newLeft, RelNode newRight) {
        RexBuilder rexBuilder = newLeft.getCluster().getRexBuilder();

        // Extract original join key ColumnRefs
        List<ColumnRef> origCols = registry.outputColumnsOf(originalJoin);
        int origLeftCount = originalJoin.getLeft().getRowType().getFieldCount();

        // Collect key pairs as ColumnRefs
        List<ColumnRef[]> keyPairs = new ArrayList<>();
        extractKeyPairs(originalJoin.getCondition(), origLeftCount, origCols, keyPairs);

        if (keyPairs.isEmpty()) {
            // Non-equi join: return with TRUE condition (best effort)
            return LogicalJoin.create(newLeft, newRight, Collections.emptyList(),
                    rexBuilder.makeLiteral(true), Collections.emptySet(),
                    originalJoin.getJoinType());
        }

        // Resolve keys in new children
        List<RexNode> equalities = new ArrayList<>();
        int newLeftCount = newLeft.getRowType().getFieldCount();

        for (ColumnRef[] pair : keyPairs) {
            int lhsIdx = registry.resolveIndex(pair[0], newLeft);
            int rhsIdx = registry.resolveIndex(pair[1], newRight);

            if (lhsIdx >= 0 && rhsIdx >= 0) {
                RexNode lhsRef = rexBuilder.makeInputRef(
                        newLeft.getRowType().getFieldList().get(lhsIdx).getType(), lhsIdx);
                RexNode rhsRef = rexBuilder.makeInputRef(
                        newRight.getRowType().getFieldList().get(rhsIdx).getType(),
                        newLeftCount + rhsIdx);
                equalities.add(rexBuilder.makeCall(SqlStdOperatorTable.EQUALS, lhsRef, rhsRef));
            }
        }

        RexNode newCondition;
        if (equalities.isEmpty()) {
            newCondition = rexBuilder.makeLiteral(true);
        } else if (equalities.size() == 1) {
            newCondition = equalities.get(0);
        } else {
            newCondition = rexBuilder.makeCall(SqlStdOperatorTable.AND, equalities);
        }

        return LogicalJoin.create(newLeft, newRight, Collections.emptyList(),
                newCondition, Collections.emptySet(), originalJoin.getJoinType());
    }

    private void extractKeyPairs(RexNode condition, int leftFieldCount,
                                  List<ColumnRef> allCols, List<ColumnRef[]> pairs) {
        if (condition instanceof org.apache.calcite.rex.RexCall) {
            org.apache.calcite.rex.RexCall call = (org.apache.calcite.rex.RexCall) condition;
            String opName = call.getOperator().getName();
            if ("=".equals(opName) && call.getOperands().size() == 2) {
                RexNode left = call.getOperands().get(0);
                RexNode right = call.getOperands().get(1);
                if (left instanceof RexInputRef && right instanceof RexInputRef) {
                    int leftIdx = ((RexInputRef) left).getIndex();
                    int rightIdx = ((RexInputRef) right).getIndex();
                    if (leftIdx < leftFieldCount && rightIdx >= leftFieldCount) {
                        pairs.add(new ColumnRef[]{allCols.get(leftIdx), allCols.get(rightIdx)});
                    } else if (rightIdx < leftFieldCount && leftIdx >= leftFieldCount) {
                        pairs.add(new ColumnRef[]{allCols.get(rightIdx), allCols.get(leftIdx)});
                    }
                }
            } else if ("AND".equals(opName)) {
                for (RexNode operand : call.getOperands()) {
                    extractKeyPairs(operand, leftFieldCount, allCols, pairs);
                }
            }
        }
    }

    private static RelNode unwrap(RelNode node) {
        while (node instanceof HepRelVertex) {
            node = ((HepRelVertex) node).getCurrentRel();
        }
        return node;
    }

    /**
     * Unwrap identity Projections to find the underlying node.
     * Identity projections are pass-through (all columns in order, no transformation).
     */
    private static RelNode unwrapIdentityProject(RelNode node) {
        node = unwrap(node);
        while (node instanceof org.apache.calcite.rel.logical.LogicalProject) {
            org.apache.calcite.rel.logical.LogicalProject proj =
                    (org.apache.calcite.rel.logical.LogicalProject) node;
            boolean isIdentity = true;
            if (proj.getProjects().size() != proj.getInput().getRowType().getFieldCount()) break;
            for (int i = 0; i < proj.getProjects().size(); i++) {
                RexNode expr = proj.getProjects().get(i);
                if (!(expr instanceof RexInputRef) || ((RexInputRef) expr).getIndex() != i) {
                    isIdentity = false;
                    break;
                }
            }
            if (!isIdentity) break;
            node = unwrap(proj.getInput());
        }
        return node;
    }
}
