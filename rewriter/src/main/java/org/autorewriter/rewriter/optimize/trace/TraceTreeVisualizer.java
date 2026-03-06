package org.autorewriter.rewriter.optimize.trace;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Exports an {@link OptimizationTrace} as a Graphviz DOT graph and renders it to PNG.
 * <p>
 * Each unique RelNode becomes a graph node; each rule application becomes a directed edge.
 * Logical nodes are yellow, physical (JDBC) nodes are blue.
 * Logical rule edges are red, conversion rule edges are blue.
 */
@Slf4j
public class TraceTreeVisualizer {

    private TraceTreeVisualizer() {}

    /**
     * Generate a DOT-format string from the trace.
     */
    public static String generateDot(OptimizationTrace trace) {
        List<RuleApplicationStep> steps = trace.getSteps();
        StringBuilder sb = new StringBuilder();

        sb.append("digraph RewriteTree {\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  graph [fontname=\"Helvetica\", fontsize=10];\n");
        sb.append("  node [shape=box, style=filled, fontname=\"Helvetica\", fontsize=9];\n");
        sb.append("  edge [fontname=\"Helvetica\", fontsize=8];\n");
        sb.append("\n");

        Set<String> declaredNodes = new LinkedHashSet<>();

        for (RuleApplicationStep step : steps) {
            RelNode from = step.getMatchedRelNode();
            RelNode to = step.getProducedRelNode();

            String fromId = nodeId(from);
            String toId = nodeId(to);

            if (declaredNodes.add(fromId)) {
                sb.append(nodeDeclaration(fromId, from));
            }
            if (declaredNodes.add(toId)) {
                sb.append(nodeDeclaration(toId, to));
            }

            boolean isConversion = step.getRule() instanceof ConverterRule;
            String ruleLabel = step.getRule().getClass().getSimpleName();
            String edgeColor = isConversion ? "\"#3366CC\"" : "\"#CC3333\"";
            String edgeStyle = isConversion ? "dashed" : "solid";

            sb.append(String.format("  %s -> %s [label=\"#%d %s\", color=%s, style=%s];\n",
                    fromId, toId, step.getStepIndex(), escape(ruleLabel),
                    edgeColor, edgeStyle));
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Export the trace as a PNG image using the Graphviz {@code dot} command.
     *
     * @param trace      the optimization trace
     * @param outputPath path of the output PNG file
     * @throws IOException if writing or rendering fails
     */
    public static void exportToPng(OptimizationTrace trace, String outputPath) throws IOException {
        String dot = generateDot(trace);

        Path dotFile = Files.createTempFile("trace-tree-", ".dot");
        try {
            Files.write(dotFile, dot.getBytes(StandardCharsets.UTF_8));

            Path outputFile = Path.of(outputPath);
            Files.createDirectories(outputFile.getParent());

            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", "-o",
                    outputFile.toAbsolutePath().toString(),
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

            log.info("Trace tree exported to {}", outputFile.toAbsolutePath());
        } finally {
            Files.deleteIfExists(dotFile);
        }
    }

    private static String nodeId(RelNode node) {
        return "n" + node.getId();
    }

    private static String nodeDeclaration(String id, RelNode node) {
        String typeName = node.getRelTypeName();
        String convention = node.getConvention() != null
                ? node.getConvention().toString() : "?";
        boolean isPhysical = convention.contains("JDBC");
        String fillColor = isPhysical ? "\"#DAEAF6\"" : "\"#FFF8DC\"";

        return String.format("  %s [label=\"%s\\nid=%d (%s)\", fillcolor=%s];\n",
                id, escape(typeName), node.getId(), escape(convention), fillColor);
    }

    private static String escape(String s) {
        return s.replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
