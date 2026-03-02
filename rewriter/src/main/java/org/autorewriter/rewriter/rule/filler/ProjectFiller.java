package org.autorewriter.rewriter.rule.filler;

import lombok.AllArgsConstructor;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rex.RexNode;
import org.autorewriter.rewriter.rule.RelNodeFiller;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.autorewriter.rewriter.rule.util.ColumnRefResolver;
import org.autorewriter.rewriter.rule.util.RexNodeFiller;

import java.util.*;
import java.util.function.BiFunction;

@AllArgsConstructor
public class ProjectFiller implements RelNodeFiller<LogicalProject> {

    private final BiFunction<RelNode, Map<String, Object>, RelNode> fillTargetTemplateFunc;
    private final RexNodeFiller rexNodeFiller;

    @Override
    @SuppressWarnings("unchecked")
    public RelNode fill(LogicalProject template, Map<String, Object> bindings) {
        RelNode filledInput = fillTargetTemplateFunc.apply(template.getInput(), bindings);

        List<String> templateFieldNames = template.getRowType().getFieldNames();

        List<String> attrPlaceholders = new ArrayList<>();
        for (String f : templateFieldNames) {
            if (f.matches("a\\d+")) {
                attrPlaceholders.add(f);
            }
        }

        boolean hasMultiColumnBinding = false;
        for (String attr : attrPlaceholders) {
            Object val = bindings.get(attr + "_colref");
            if (val instanceof List) {
                hasMultiColumnBinding = true;
                break;
            }
            Object val2 = bindings.get(attr);
            if (val2 instanceof List) {
                hasMultiColumnBinding = true;
                break;
            }
        }

        if (hasMultiColumnBinding) {
            List<RexNode> filledProjects = new ArrayList<>();
            List<String> filledFieldNames = new ArrayList<>();

            for (String fieldName : templateFieldNames) {
                if (fieldName.matches("a\\d+")) {
                    Object colRefsObj = bindings.get(fieldName + "_colref");
                    Object nameObj = bindings.get(fieldName);
                    Object projectsObj = bindings.get(fieldName + "_projects");

                    if (colRefsObj instanceof List) {
                        List<ColumnRef> colRefs = (List<ColumnRef>) colRefsObj;
                        List<String> names = nameObj instanceof List ? (List<String>) nameObj : null;
                        List<RexNode> projects = projectsObj instanceof List ? (List<RexNode>) projectsObj : null;

                        for (int j = 0; j < colRefs.size(); j++) {
                            ColumnRef cr = colRefs.get(j);
                            int idx = ColumnRefResolver.resolveIndex(cr, filledInput);
                            if (projects != null && j < projects.size()) {
                                filledProjects.add(rexNodeFiller.fill(projects.get(j), bindings, filledInput));
                            } else if (idx >= 0 && idx < filledInput.getRowType().getFieldCount()) {
                                filledProjects.add(filledInput.getCluster().getRexBuilder().makeInputRef(
                                        filledInput.getRowType().getFieldList().get(idx).getType(), idx));
                            }
                            if (names != null && j < names.size()) {
                                filledFieldNames.add(names.get(j));
                            } else {
                                filledFieldNames.add(cr.getColumnName());
                            }
                        }
                    }
                } else if (!fieldName.matches("s\\d+")) {
                    filledFieldNames.add(fieldName);
                    int fieldIdx = templateFieldNames.indexOf(fieldName);
                    if (fieldIdx < template.getProjects().size()) {
                        filledProjects.add(rexNodeFiller.fill(template.getProjects().get(fieldIdx), bindings, filledInput));
                    }
                }
            }

            return LogicalProject.create(filledInput, Collections.emptyList(), filledProjects, filledFieldNames);
        }

        List<RexNode> filledProjects = new ArrayList<>();
        for (RexNode project : template.getProjects()) {
            filledProjects.add(rexNodeFiller.fill(project, bindings, filledInput));
        }

        List<String> filledFieldNames = new ArrayList<>();
        for (String fieldName : templateFieldNames) {
            if (fieldName.matches("a\\d+") && bindings.containsKey(fieldName)) {
                Object boundValue = bindings.get(fieldName);
                if (boundValue instanceof String) {
                    filledFieldNames.add((String) boundValue);
                } else {
                    filledFieldNames.add(fieldName);
                }
            } else if (!fieldName.matches("s\\d+")) {
                filledFieldNames.add(fieldName);
            }
        }

        return LogicalProject.create(filledInput, Collections.emptyList(), filledProjects, filledFieldNames);
    }
}
