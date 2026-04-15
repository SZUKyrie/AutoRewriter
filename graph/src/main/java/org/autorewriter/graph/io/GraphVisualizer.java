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
     */
    public static String generateDot(RuleDependencyGraph graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph RuleDependencyGraph {\n");
        sb.append("  rankdir=LR;\n");
        sb.append("  graph [fontname=\"Helvetica\", fontsize=10];\n");
        sb.append("  node  [shape=box, style=filled, fontname=\"Helvetica\", fontsize=9];\n");
        sb.append("  edge  [fontname=\"Helvetica\", fontsize=8];\n\n");

        // Compute max observationCount for color scaling
        int maxObs = graph.getNodes().values().stream()
                .mapToInt(RuleNode::getObservationCount)
                .max().orElse(1);

        // Emit nodes
        for (RuleNode node : graph.getNodes().values()) {
            String label = String.format("rule_%d\\n%s\\nfired: %d",
                    node.getRuleId(),
                    truncate(node.getSourceTemplateSignature(), 30),
                    node.getObservationCount());

            double ratio = maxObs == 0 ? 0.0 : (double) node.getObservationCount() / maxObs;
            String fillColor = interpolateColor(ratio);

            sb.append(String.format("  n%d [label=\"%s\", fillcolor=\"%s\"];\n",
                    node.getRuleId(), label, fillColor));
        }

        sb.append("\n");

        // Emit edges
        for (Map.Entry<Integer, List<DependencyEdge>> entry : graph.getOutEdges().entrySet()) {
            int fromId = entry.getKey();
            RuleNode fromNode = graph.getNode(fromId);
            int fromObs = fromNode != null ? fromNode.getObservationCount() : 0;

            for (DependencyEdge edge : entry.getValue()) {
                double prob      = edge.getProbability(fromObs);
                double benefit   = edge.getAvgBenefit();
                double penwidth  = Math.min(5.0, 1.0 + edge.getFireCount() * 0.1);
                String edgeLabel = String.format("p=%.2f\\nbenefit=%.0f", prob, benefit);

                sb.append(String.format(
                        "  n%d -> n%d [label=\"%s\", penwidth=%.1f, color=\"#CC3333\"];\n",
                        edge.getFromRuleId(), edge.getToRuleId(), edgeLabel, penwidth));
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

    /** Truncate a string to maxLen chars, appending "..." if truncated. */
    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
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
