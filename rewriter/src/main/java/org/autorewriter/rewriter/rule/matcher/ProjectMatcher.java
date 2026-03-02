package org.autorewriter.rewriter.rule.matcher;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.autorewriter.rewriter.rule.RelNodeMatcher;
import org.autorewriter.rewriter.rule.util.ColumnRef;
import org.autorewriter.rewriter.rule.util.ColumnRefResolver;
import org.autorewriter.rewriter.rule.util.RexNodeMatcher;

import java.util.ArrayList;
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

        List<String> templateFields = template.getRowType().getFieldNames();
        List<RexNode> queryProjects = query.getProjects();
        List<String> queryFields = query.getRowType().getFieldNames();

        List<String> attrPlaceholders = new ArrayList<>();
        String schemaPlaceholder = null;
        for (String f : templateFields) {
            if (f.matches("a\\d+")) {
                attrPlaceholders.add(f);
            } else if (f.matches("s\\d+")) {
                schemaPlaceholder = f;
            }
        }

        if (attrPlaceholders.size() == 1) {
            String attr = attrPlaceholders.get(0);
            List<String> allFieldNames = new ArrayList<>(queryFields);
            List<ColumnRef> allColRefs = new ArrayList<>();
            List<RexNode> allProjectExprs = new ArrayList<>();
            for (int i = 0; i < queryProjects.size(); i++) {
                RexNode proj = queryProjects.get(i);
                if (proj instanceof RexInputRef) {
                    allColRefs.add(ColumnRefResolver.resolve(((RexInputRef) proj).getIndex(), query.getInput()));
                } else {
                    allColRefs.add(new ColumnRef("$expr", queryFields.get(i)));
                }
                allProjectExprs.add(proj);
            }
            bindings.put(attr, allFieldNames);
            bindings.put(attr + "_colref", allColRefs);
            bindings.put(attr + "_projects", allProjectExprs);
        } else {
            rexNodeMatcher.setQueryOperator(query.getInput());
            List<RexNode> templateProjects = template.getProjects();
            if (templateProjects.size() != queryProjects.size()) {
                return false;
            }
            for (int i = 0; i < templateProjects.size(); i++) {
                if (!rexNodeMatcher.match(templateProjects.get(i), queryProjects.get(i), bindings)) {
                    return false;
                }
            }
            for (int i = 0; i < templateFields.size(); i++) {
                String tf = templateFields.get(i);
                if (tf.matches("a\\d+")) {
                    bindings.put(tf, queryFields.get(i));
                }
            }
        }

        if (schemaPlaceholder != null) {
            bindings.put(schemaPlaceholder, queryFields);
        }

        return true;
    }
}
