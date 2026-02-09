package org.autorewriter.rewriter.rule.matcher;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rex.RexNode;
import org.autorewriter.rewriter.rule.RelNodeMatcher;
import org.autorewriter.rewriter.rule.util.RexNodeMatcher;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Slf4j
@AllArgsConstructor
public class ProjectMatcher implements RelNodeMatcher<LogicalProject> {

    private final BiFunction<org.apache.calcite.rel.RelNode, org.apache.calcite.rel.RelNode, Boolean> recursiveMatchFunc;
    private final RexNodeMatcher rexNodeMatcher;

    @Override
    public boolean match(LogicalProject template, LogicalProject query, Map<String, Object> bindings) {
        if (!recursiveMatchFunc.apply(template.getInput(), query.getInput())) {
            return false;
        }

        List<RexNode> templateProjects = template.getProjects();
        List<RexNode> queryProjects = query.getProjects();

        if (templateProjects.size() != queryProjects.size()) {
            return false;
        }

        for (int i = 0; i < templateProjects.size(); i++) {
            if (!rexNodeMatcher.match(templateProjects.get(i), queryProjects.get(i), bindings)) {
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
}
