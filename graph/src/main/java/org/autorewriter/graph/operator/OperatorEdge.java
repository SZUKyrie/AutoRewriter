package org.autorewriter.graph.operator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 规则算子图中的边，有两种类型：
 *
 * <ul>
 *   <li>{@link EdgeType#STRUCTURAL}：结构边，表示规则算子树中父子关系（parent → child）。</li>
 *   <li>{@link EdgeType#TRANSFORM}：转换边，表示从源模版根节点到目标模版根节点的改写关系，
 *       边上携带转换约束描述。</li>
 * </ul>
 */
@Getter
public class OperatorEdge {

    public enum EdgeType { STRUCTURAL, TRANSFORM }

    private final String    fromNodeId;
    private final String    toNodeId;
    private final EdgeType  type;

    /**
     * 仅 TRANSFORM 边有效：对应规则的 ID。
     */
    private final int       ruleId;

    /**
     * 仅 TRANSFORM 边有效：转换约束的可读描述（如 "Unique(t,a); NotNull(t,b)"）。
     */
    private final String    constraintDesc;

    @JsonCreator
    public OperatorEdge(
            @JsonProperty("fromNodeId")      String    fromNodeId,
            @JsonProperty("toNodeId")        String    toNodeId,
            @JsonProperty("type")            EdgeType  type,
            @JsonProperty("ruleId")          int       ruleId,
            @JsonProperty("constraintDesc")  String    constraintDesc) {
        this.fromNodeId     = fromNodeId;
        this.toNodeId       = toNodeId;
        this.type           = type;
        this.ruleId         = ruleId;
        this.constraintDesc = constraintDesc;
    }

    /** 创建结构边（父→子）。 */
    public static OperatorEdge structural(String parentId, String childId) {
        return new OperatorEdge(parentId, childId, EdgeType.STRUCTURAL, -1, null);
    }

    /** 创建转换边（源模版根→目标模版根）。 */
    public static OperatorEdge transform(String srcRootId, String tgtRootId,
                                         int ruleId, String constraintDesc) {
        return new OperatorEdge(srcRootId, tgtRootId, EdgeType.TRANSFORM, ruleId, constraintDesc);
    }
}
