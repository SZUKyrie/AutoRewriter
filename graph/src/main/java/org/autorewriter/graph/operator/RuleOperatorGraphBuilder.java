package org.autorewriter.graph.operator;

import org.apache.calcite.rel.RelNode;
import org.autorewriter.rewriter.rule.AutoRewriteRule;

import java.util.*;

/**
 * 从 {@link AutoRewriteRule} 列表构建 {@link RuleOperatorGraph}。
 *
 * <p>每调用一次 {@link #addRule(AutoRewriteRule)} 即完成：
 * <ol>
 *   <li>将源模版算子树的所有节点（按 explain 去重）加入图；</li>
 *   <li>对源模版树添加结构边（父→子）；</li>
 *   <li>将目标模版算子树的所有节点加入图（同样去重）；</li>
 *   <li>对目标模版树添加结构边；</li>
 *   <li>添加从源模版根到目标模版根的转换边，边上携带约束描述。</li>
 * </ol>
 */
public class RuleOperatorGraphBuilder {

    private final Map<String, OperatorNode> nodes = new LinkedHashMap<>();

    /** (fromId|toId|type|ruleId) → edge，防止重复添加相同边。 */
    private final Map<String, OperatorEdge> edges = new LinkedHashMap<>();

    /**
     * 注册一条规则，将其源/目标模版树加入图。
     *
     * @param rule AutoRewriteRule（含 sourceTemplate、targetTemplate 和 constraints）
     */
    public void addRule(AutoRewriteRule rule) {
        RelNode src = rule.getSourceTemplate();
        RelNode tgt = rule.getTargetTemplate();
        int ruleId  = rule.getRuleId();

        // 构建约束描述（直接 toString 即可）
        String constraintDesc = rule.toString();

        // 注册源模版树
        String srcRootId = registerTree(src);
        // 注册目标模版树
        String tgtRootId = registerTree(tgt);

        // 添加转换边（源根 → 目标根）
        addEdge(OperatorEdge.transform(srcRootId, tgtRootId, ruleId, constraintDesc));
    }

    /** 递归注册算子树，返回根节点的 nodeId。 */
    private String registerTree(RelNode node) {
        String explain = node.explain();
        String nodeId  = OperatorNode.idOf(explain);

        // 节点去重
        nodes.putIfAbsent(nodeId, new OperatorNode(nodeId, node.getRelTypeName(), explain));

        // 子节点
        for (RelNode child : node.getInputs()) {
            String childId = registerTree(child);
            addEdge(OperatorEdge.structural(nodeId, childId));
        }
        return nodeId;
    }

    private void addEdge(OperatorEdge edge) {
        String key = edge.getFromNodeId() + "|" + edge.getToNodeId()
                + "|" + edge.getType() + "|" + edge.getRuleId();
        edges.putIfAbsent(key, edge);
    }

    /** 构建最终图。 */
    public RuleOperatorGraph build() {
        return new RuleOperatorGraph(new LinkedHashMap<>(nodes), new ArrayList<>(edges.values()));
    }
}
