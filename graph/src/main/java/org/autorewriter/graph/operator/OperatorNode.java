package org.autorewriter.graph.operator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 规则算子图中的节点，代表一个 Calcite LogicalNode（规则模版中的算子）。
 *
 * <p>节点 ID 由 {@code RelNode.explain()} 的内容决定，保证相同语义的算子复用同一节点。
 */
@Getter
public class OperatorNode {

    /** 节点唯一 ID，等于对应 RelNode 的 explain() 文本（去空白后的 hash + 摘要）。 */
    private final String nodeId;

    /** RelNode 的类型名，如 LogicalProject, LogicalFilter 等。 */
    private final String relTypeName;

    /** RelNode.explain() 的完整文本，供可视化展示。 */
    private final String explainText;

    @JsonCreator
    public OperatorNode(
            @JsonProperty("nodeId")      String nodeId,
            @JsonProperty("relTypeName") String relTypeName,
            @JsonProperty("explainText") String explainText) {
        this.nodeId      = nodeId;
        this.relTypeName = relTypeName;
        this.explainText = explainText;
    }

    /**
     * 从 RelNode.explain() 生成节点 ID。
     * 取前 200 字符的 MD5 hex 作为短 ID，避免超长字符串。
     */
    public static String idOf(String explainText) {
        // 规范化：去除多余空白
        String normalized = explainText.replaceAll("\\s+", " ").trim();
        // 用 MD5 前 16 位作为 ID
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // 降级：直接用截断文本
            return normalized.substring(0, Math.min(64, normalized.length()));
        }
    }
}
