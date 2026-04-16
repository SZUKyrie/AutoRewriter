package org.autorewriter.graph.operator;

import org.autorewriter.graph.operator.OperatorEdge.EdgeType;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 将 {@link RuleOperatorGraph} 导出为 Graphviz DOT 格式。
 *
 * <p>布局说明：
 * <ul>
 *   <li>结构边（STRUCTURAL）：实线黑色箭头，表示算子父→子关系。</li>
 *   <li>转换边（TRANSFORM）：粗彩色虚线箭头，边标签显示规则 ID。</li>
 *   <li>节点标签：显示 relTypeName + explain 摘要（前 40 字符）。</li>
 * </ul>
 */
public class RuleOperatorGraphVisualizer {

    private RuleOperatorGraphVisualizer() {}

    public static String generateDot(RuleOperatorGraph graph) {
        if (graph.nodeCount() == 0) {
            return "digraph RuleOperatorGraph { label=\"empty graph\"; }\n";
        }

        // 转换边颜色轮换（区分不同规则）
        String[] COLORS = { "#CC2222", "#1A7FCC", "#2E8B00", "#CC6600",
                            "#7B22CC", "#CC0077", "#008B8B", "#8B6914" };

        // 每条转换边分配一个颜色（按 ruleId 取模）
        Map<Integer, String> ruleColor = new LinkedHashMap<>();
        int colorIdx = 0;
        for (OperatorEdge e : graph.transformEdges()) {
            if (!ruleColor.containsKey(e.getRuleId())) {
                ruleColor.put(e.getRuleId(), COLORS[colorIdx % COLORS.length]);
                colorIdx++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("digraph RuleOperatorGraph {\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  splines=false;\n");
        sb.append("  nodesep=0.4;\n");
        sb.append("  ranksep=0.6;\n");
        sb.append("  graph [fontname=\"Helvetica\", fontsize=11];\n");
        sb.append("  node  [shape=box, style=\"filled,rounded\", fontname=\"Helvetica-Bold\", fontsize=10];\n");
        sb.append("  edge  [fontname=\"Helvetica\", fontsize=8, arrowsize=0.6];\n\n");

        // ── 节点 ──────────────────────────────────────────────────────────────
        for (OperatorNode node : graph.getNodes().values()) {
            String dotId = sanitize(node.getNodeId());
            // 显示：类型名 + explain 摘要
            String summary = node.getExplainText()
                    .replaceAll("\\s+", " ").trim();
            if (summary.length() > 50) summary = summary.substring(0, 47) + "...";
            String typeName = node.getRelTypeName().replace("Logical", "");
            String label = typeName + "\\n" + escape(summary);
            String fill = "#E8F4FF";  // 浅蓝：算子节点
            sb.append(String.format("  %s [label=\"%s\", fillcolor=\"%s\"];\n",
                    dotId, label, fill));
        }
        sb.append("\n");

        // ── 结构边（实线黑色）─────────────────────────────────────────────────
        for (OperatorEdge e : graph.structuralEdges()) {
            sb.append(String.format("  %s -> %s [color=\"#444444\", penwidth=1.0];\n",
                    sanitize(e.getFromNodeId()), sanitize(e.getToNodeId())));
        }
        sb.append("\n");

        // ── 转换边（粗彩色虚线，标注规则 ID）─────────────────────────────────
        for (OperatorEdge e : graph.transformEdges()) {
            String color = ruleColor.getOrDefault(e.getRuleId(), "#888888");
            String label = "r" + e.getRuleId();
            sb.append(String.format(
                    "  %s -> %s [label=\"%s\", color=\"%s\", fontcolor=\"%s\", "
                    + "penwidth=2.0, style=dashed, constraint=false];\n",
                    sanitize(e.getFromNodeId()), sanitize(e.getToNodeId()),
                    label, color, color));
        }

        sb.append("}\n");
        return sb.toString();
    }

    public static void exportToDot(RuleOperatorGraph graph, Path outputPath) throws IOException {
        if (outputPath.getParent() != null) Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, generateDot(graph).getBytes(StandardCharsets.UTF_8));
    }

    public static void exportToPng(RuleOperatorGraph graph, Path outputPath) throws IOException {
        exportWithFormat(graph, outputPath, "png");
    }

    /** 导出为 SVG 矢量图（可任意缩放，节点文字清晰）。 */
    public static void exportToSvg(RuleOperatorGraph graph, Path outputPath) throws IOException {
        exportWithFormat(graph, outputPath, "svg");
    }

    /** 导出为 PDF 矢量图。 */
    public static void exportToPdf(RuleOperatorGraph graph, Path outputPath) throws IOException {
        exportWithFormat(graph, outputPath, "pdf");
    }

    private static void exportWithFormat(RuleOperatorGraph graph, Path outputPath,
                                         String format) throws IOException {
        String dot = generateDot(graph);
        Path dotFile = Files.createTempFile("rule-op-graph-", ".dot");
        try {
            Files.write(dotFile, dot.getBytes(StandardCharsets.UTF_8));
            if (outputPath.getParent() != null) Files.createDirectories(outputPath.getParent());
            ProcessBuilder pb = new ProcessBuilder(
                    "dot", "-T" + format, "-o",
                    outputPath.toAbsolutePath().toString(),
                    dotFile.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit;
            try { exit = proc.waitFor(); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted waiting for dot", e);
            }
            if (exit != 0) throw new IOException("dot failed (exit " + exit + "): " + out);
        } finally {
            Files.deleteIfExists(dotFile);
        }
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    private static String sanitize(String id) {
        return "n" + id.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", "\\n");
    }
}
