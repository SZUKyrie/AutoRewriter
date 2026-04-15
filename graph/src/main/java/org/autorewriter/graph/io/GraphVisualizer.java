package org.autorewriter.graph.io;

import lombok.extern.slf4j.Slf4j;
import org.autorewriter.graph.model.DependencyEdge;
import org.autorewriter.graph.model.RuleDependencyGraph;
import org.autorewriter.graph.model.RuleNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
/**
 * Exports a {@link RuleDependencyGraph} as a Graphviz DOT graph and renders it to PNG.
 *
 * <p>Nodes represent AutoRewriteRules; directed edges represent observed firing sequences.
 * Node color reflects firing frequency; edge width reflects transition count.
 *
 * <p>Requires the {@code dot} command (Graphviz) to be installed for PNG export.
 */
@Slf4j
public class GraphVisualizer {

    private GraphVisualizer() {}

    /**
     * Generate a DOT-format string from the rule dependency graph.
     *
     * <p>Layout strategy for tree-like clarity:
     * <ul>
     *   <li>{@code rankdir=TB} — top-to-bottom flow (temporal order)</li>
     *   <li>Nodes ranked by in-degree: roots (in-degree=0) at top</li>
     *   <li>Nodes grouped by ruleId type (same matchedNodeSignature prefix → same subgraph cluster)</li>
     *   <li>Edges with high fireCount are thicker and darker</li>
     *   <li>Low-weight edges (fireCount=1) are drawn lighter to reduce visual noise</li>
     * </ul>
     */
    public static String generateDot(RuleDependencyGraph graph) {
        if (graph.nodeCount() == 0) {
            return "digraph RuleDependencyGraph { label=\"empty graph\"; }\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("digraph RuleDependencyGraph {\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  splines=polyline;\n");
        sb.append("  nodesep=0.5;\n");
        sb.append("  ranksep=1.2;\n");
        sb.append("  graph [fontname=\"Helvetica\", fontsize=11];\n");
        sb.append("  node  [shape=box, style=\"filled,rounded\", fontname=\"Helvetica\", fontsize=9, width=1.8, fixedsize=false];\n");
        sb.append("  edge  [fontname=\"Helvetica\", fontsize=7, arrowsize=0.5];\n\n");

        // --- compute in-degree ---
        Map<String, Integer> inDegree = new java.util.HashMap<>();
        for (RuleNode node : graph.getNodes().values()) {
            inDegree.put(node.getNodeKey(), 0);
        }
        for (List<DependencyEdge> edges : graph.getOutEdges().values()) {
            for (DependencyEdge e : edges) {
                inDegree.merge(e.getToNodeKey(), 1, Integer::sum);
            }
        }

        // --- max fire count for edge scaling ---
        int maxFire = graph.getOutEdges().values().stream()
                .flatMap(List::stream)
                .mapToInt(DependencyEdge::getFireCount)
                .max().orElse(1);

        // --- max observation count for color scaling ---
        int maxObs = graph.getNodes().values().stream()
                .mapToInt(RuleNode::getObservationCount)
                .max().orElse(1);

        // --- roots rank ---
        List<String> roots = graph.getNodes().keySet().stream()
                .filter(k -> inDegree.getOrDefault(k, 0) == 0)
                .collect(java.util.stream.Collectors.toList());
        if (!roots.isEmpty()) {
            sb.append("  { rank=source; ");
            for (String k : roots) sb.append(sanitizeDotId(k)).append("; ");
            sb.append("}\n\n");
        }

        // --- cluster by matched node type ---
        Map<String, List<RuleNode>> byType = new java.util.LinkedHashMap<>();
        for (RuleNode node : graph.getNodes().values()) {
            String sig  = node.getMatchedNodeSignature();
            String type = sig.contains("-") ? sig.substring(0, sig.indexOf('-')) : sig;
            byType.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(node);
        }

        int clusterIdx = 0;
        for (Map.Entry<String, List<RuleNode>> entry : byType.entrySet()) {
            List<RuleNode> clusterNodes = entry.getValue();
            if (clusterNodes.size() > 1) {
                sb.append(String.format("  subgraph cluster_%d {\n", clusterIdx++));
                sb.append(String.format("    label=\"%s\";\n", escape(entry.getKey())));
                sb.append("    style=dashed; color=\"#BBBBBB\"; bgcolor=\"#F8F8F8\";\n");
                for (RuleNode node : clusterNodes) {
                    sb.append("    ").append(sanitizeDotId(node.getNodeKey())).append(";\n");
                }
                sb.append("  }\n");
            }
        }
        sb.append("\n");

        // --- emit nodes ---
        for (RuleNode node : graph.getNodes().values()) {
            String dotId = sanitizeDotId(node.getNodeKey());
            String matchedType = node.getMatchedNodeSignature();
            if (matchedType.contains("-")) matchedType = matchedType.substring(0, matchedType.indexOf('-'));
            // Remove "Logical" prefix for brevity
            matchedType = matchedType.replace("Logical", "");

            String label = String.format("r%d / %s\\n×%d",
                    node.getRuleId(), escape(matchedType), node.getObservationCount());

            double ratio     = maxObs == 0 ? 0.0 : (double) node.getObservationCount() / maxObs;
            String fillColor = interpolateColor(ratio);
            boolean isRoot   = inDegree.getOrDefault(node.getNodeKey(), 0) == 0;
            String border    = isRoot ? ", penwidth=2.5, color=\"#444444\"" : ", penwidth=0.8, color=\"#BBBBBB\"";

            sb.append(String.format("  %s [label=\"%s\", fillcolor=\"%s\"%s];\n",
                    dotId, label, fillColor, border));
        }
        sb.append("\n");

        // --- emit only high-weight edges to reduce clutter ---
        // Show edges where fireCount >= threshold (top ~30% of edges)
        int[] allFires = graph.getOutEdges().values().stream()
                .flatMap(List::stream)
                .mapToInt(DependencyEdge::getFireCount)
                .sorted().toArray();
        int threshold = allFires.length > 0
                ? allFires[Math.max(0, (int)(allFires.length * 0.5))]  // top 50%
                : 1;

        for (Map.Entry<String, List<DependencyEdge>> entry : graph.getOutEdges().entrySet()) {
            String fromKey  = entry.getKey();
            RuleNode fromNode = graph.getNode(fromKey);
            int fromObs = fromNode != null ? fromNode.getObservationCount() : 0;

            for (DependencyEdge edge : entry.getValue()) {
                if (edge.getFireCount() < threshold) continue;  // skip low-weight edges

                double prob    = edge.getProbability(fromObs);
                int    fire    = edge.getFireCount();
                double penwidth = 0.8 + (double)(fire - threshold) / Math.max(1, maxFire - threshold) * 2.5;
                String color   = prob >= 0.7 ? "#CC2222"
                               : prob >= 0.4 ? "#888888"
                               :               "#BBBBBB";
                String lbl = fire > 1 ? String.format("×%d", fire) : "";

                sb.append(String.format(
                        "  %s -> %s [label=\"%s\", penwidth=%.1f, color=\"%s\", fontcolor=\"%s\"];\n",
                        sanitizeDotId(edge.getFromNodeKey()),
                        sanitizeDotId(edge.getToNodeKey()),
                        lbl, penwidth, color, color));
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Export the graph as a DOT file.
     */
    public static void exportToDot(RuleDependencyGraph graph, Path outputPath) throws IOException {
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        Files.write(outputPath, generateDot(graph).getBytes(StandardCharsets.UTF_8));
        log.info("Graph DOT exported to {}", outputPath);
    }

    /**
     * Export the graph as a PNG image using the Graphviz {@code dot} command.
     *
     * @param graph      the rule dependency graph
     * @param outputPath path of the output PNG file
     * @throws IOException if writing or rendering fails
     */
    public static void exportToPng(RuleDependencyGraph graph, Path outputPath) throws IOException {
        String dot = generateDot(graph);

        Path dotFile = Files.createTempFile("rule-graph-", ".dot");
        try {
            Files.write(dotFile, dot.getBytes(StandardCharsets.UTF_8));

            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "dot", "-Tpng", "-o",
                    outputPath.toAbsolutePath().toString(),
                    dotFile.toAbsolutePath().toString());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String processOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for dot process", e);
            }

            if (exitCode != 0) {
                throw new IOException("dot command failed (exit " + exitCode + "): " + processOutput);
            }

            log.info("Graph PNG exported to {}", outputPath.toAbsolutePath());
        } finally {
            Files.deleteIfExists(dotFile);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Escape a string for DOT label. */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", "\\n");
    }

    /** Truncate a string to maxLen chars, appending "..." if truncated. */
    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    /**
     * Convert a nodeKey ("ruleId:matchedSig") to a valid DOT identifier.
     * Replaces non-alphanumeric characters with underscores and prepends "n".
     */
    private static String sanitizeDotId(String nodeKey) {
        if (nodeKey == null) return "n_unknown";
        return "n" + nodeKey.replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * Interpolate between light yellow (#FFF8DC) and deep orange (#FF8C00)
     * based on ratio in [0.0, 1.0].
     */
    private static String interpolateColor(double ratio) {
        // Light: R=255, G=248, B=220  →  Deep: R=255, G=140, B=0
        int r = 255;
        int g = (int) (248 - ratio * (248 - 140));
        int b = (int) (220 - ratio * 220);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
