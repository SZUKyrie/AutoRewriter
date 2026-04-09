package org.autorewriter.rewriter.optimize.costBaseOpt.postgres;

import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttleImpl;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.rex.RexSubQuery;

import java.util.List;

/**
 * Post-processor that removes identity projections (projects that output
 * all input columns in order without any transformation).
 * <p>
 * Before: Project($0, $1, $2, ..., $N) → Input[N+1 cols]
 * After:  Input
 * <p>
 * Also descends into RexSubQuery to clean subquery RelNode trees.
 * <p>
 * Usage: call {@code RedundantProjectRemover.remove(relNode)} after optimization
 * and before SQL generation.
 */
public class RedundantProjectRemover extends RelShuttleImpl {

    private final RexShuttle rexShuttle = new RexShuttle() {
        @Override
        public RexNode visitSubQuery(RexSubQuery subQuery) {
            RelNode newRel = subQuery.rel.accept(RedundantProjectRemover.this);
            if (newRel != subQuery.rel) {
                return subQuery.clone(newRel);
            }
            return subQuery;
        }
    };

    public static RelNode remove(RelNode root) {
        return root.accept(new RedundantProjectRemover());
    }

    @Override
    public RelNode visit(LogicalProject project) {
        // First, process children recursively
        RelNode newInput = project.getInput().accept(this);
        // Process subqueries in project expressions
        List<RexNode> newProjects = new java.util.ArrayList<>(project.getProjects().size());
        boolean exprChanged = false;
        for (RexNode expr : project.getProjects()) {
            RexNode newExpr = expr.accept(rexShuttle);
            newProjects.add(newExpr);
            if (newExpr != expr) exprChanged = true;
        }

        Project current;
        if (newInput != project.getInput() || exprChanged) {
            current = project.copy(project.getTraitSet(), newInput, newProjects, project.getRowType());
        } else {
            current = project;
        }

        if (isIdentity(current, newInput)) {
            return newInput;
        }
        return current;
    }

    @Override
    public RelNode visit(RelNode other) {
        RelNode unwrapped = unwrap(other);
        if (unwrapped != other) {
            return unwrapped.accept(this);
        }
        if (other instanceof Project && !(other instanceof LogicalProject)) {
            // Handle non-LogicalProject subclasses (e.g., JdbcProject)
            RelNode visited = super.visit(other);
            RelNode unwrappedVisited = unwrap(visited);
            if (unwrappedVisited instanceof Project) {
                Project proj = (Project) unwrappedVisited;
                RelNode input = proj.getInput();
                RelNode unwrappedInput = unwrap(input);
                if (isIdentity(proj, unwrappedInput)) {
                    return unwrappedInput;
                }
            }
            return visited;
        }
        return super.visit(other);
    }

    /**
     * Check if a project is an identity projection: outputs all input columns
     * in the same order with no expressions.
     * <p>
     * Field name differences are ignored because parent nodes reference columns
     * by index ($i), not by name. Rewrite rules often produce identity projections
     * whose field names differ from the input (e.g. "id0" vs "id10") due to
     * Calcite's automatic deduplication of field names at different tree depths.
     */
    private static boolean isIdentity(Project project, RelNode input) {
        RelNode unwrappedInput = unwrap(input);
        RelDataType inputType = unwrappedInput.getRowType();
        List<RexNode> projects = project.getProjects();

        // Column count must match
        if (projects.size() != inputType.getFieldCount()) {
            return false;
        }

        // Each project expression must be a simple $i reference in order
        for (int i = 0; i < projects.size(); i++) {
            RexNode expr = projects.get(i);
            if (!(expr instanceof RexInputRef)) {
                return false;
            }
            if (((RexInputRef) expr).getIndex() != i) {
                return false;
            }
        }

        return true;
    }

    private static RelNode unwrap(RelNode node) {
        while (node instanceof HepRelVertex) {
            node = ((HepRelVertex) node).getCurrentRel();
        }
        return node;
    }
}
