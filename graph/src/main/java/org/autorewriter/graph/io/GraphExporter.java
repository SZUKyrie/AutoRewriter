package org.autorewriter.graph.io;

import lombok.extern.slf4j.Slf4j;
import org.autorewriter.graph.model.DependencyEdge;
import org.autorewriter.graph.model.RuleDependencyGraph;
import org.autorewriter.graph.model.RuleNode;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Exports a {@link RuleDependencyGraph} to CSV files for Python GNN training.
 * Produces nodes.csv and edges.csv in the output directory.
 */
@Slf4j
public class GraphExporter {

    public void exportForGNN(RuleDependencyGraph graph, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        exportNodes(graph, outputDir.resolve("nodes.csv"));
        exportEdges(graph, outputDir.resolve("edges.csv"));
        log.info("Graph exported to {} ({} nodes, {} edges)",
                outputDir, graph.nodeCount(), graph.edgeCount());
    }

    private void exportNodes(RuleDependencyGraph graph, Path file) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("nodeKey,ruleId,sourceSignature,targetSignature,matchedNodeSignature,observationCount,fireRate");
            w.newLine();
            for (RuleNode node : graph.getNodes().values()) {
                int totalOutFires = graph.getOutEdgesOf(node.getNodeKey())
                        .stream().mapToInt(DependencyEdge::getFireCount).sum();
                double fireRate = node.getObservationCount() == 0 ? 0.0
                        : (double) totalOutFires / node.getObservationCount();
                w.write(String.format("%s,%d,%s,%s,%s,%d,%f",
                        escapeCsv(node.getNodeKey()),
                        node.getRuleId(),
                        escapeCsv(node.getSourceTemplateSignature()),
                        escapeCsv(node.getTargetTemplateSignature()),
                        escapeCsv(node.getMatchedNodeSignature()),
                        node.getObservationCount(),
                        fireRate));
                w.newLine();
            }
        }
    }

    private void exportEdges(RuleDependencyGraph graph, Path file) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("fromNodeKey,toNodeKey,fireCount,probability,avgBenefit");
            w.newLine();
            // 直接从 JGraphT 图遍历所有边
            for (DefaultWeightedEdge je : graph.jgrapht().edgeSet()) {
                String fromKey = graph.jgrapht().getEdgeSource(je);
                String toKey   = graph.jgrapht().getEdgeTarget(je);
                // fireCount 存在边权重中
                int fireCount = (int) graph.jgrapht().getEdgeWeight(je);
                RuleNode fromNode = graph.getNode(fromKey);
                int fromObs = fromNode != null ? fromNode.getObservationCount() : 0;

                // 从 edgeMap 取完整 DependencyEdge 以拿到 avgBenefit
                double avgBenefit = graph.getOutEdgesOf(fromKey).stream()
                        .filter(e -> e.getToNodeKey().equals(toKey))
                        .mapToDouble(DependencyEdge::getAvgBenefit)
                        .findFirst().orElse(0.0);
                double probability = fromObs == 0 ? 0.0 : (double) fireCount / fromObs;

                w.write(String.format("%s,%s,%d,%f,%f",
                        escapeCsv(fromKey),
                        escapeCsv(toKey),
                        fireCount,
                        probability,
                        avgBenefit));
                w.newLine();
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
