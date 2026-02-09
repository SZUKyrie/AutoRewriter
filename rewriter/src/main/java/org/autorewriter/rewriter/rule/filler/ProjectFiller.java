package org.autorewriter.rewriter.rule.filler;

import lombok.AllArgsConstructor;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rex.RexNode;
import org.autorewriter.rewriter.rule.RelNodeFiller;
import org.autorewriter.rewriter.rule.util.RexNodeFiller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@AllArgsConstructor
public class ProjectFiller implements RelNodeFiller<LogicalProject> {

    private final BiFunction<RelNode, Map<String, Object>, RelNode> fillTargetTemplateFunc;
    private final RexNodeFiller rexNodeFiller;

    @Override
    public RelNode fill(LogicalProject template, Map<String, Object> bindings) {
        RelNode filledInput = fillTargetTemplateFunc.apply(template.getInput(), bindings);

        List<RexNode> filledProjects = new ArrayList<>();
        for (RexNode project : template.getProjects()) {
            filledProjects.add(rexNodeFiller.fill(project, bindings, filledInput));
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
}
