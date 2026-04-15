package org.autorewriter.graph.io;

import lombok.extern.slf4j.Slf4j;
import org.autorewriter.graph.model.DependencyEdge;
import org.autorewriter.graph.model.RuleDependencyGraph;
import org.autorewriter.graph.model.RuleNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
                int totalOutFires = graph.getOutEdges(node.getNodeKey())
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
            for (Map.Entry<String, List<DependencyEdge>> entry : graph.getOutEdges().entrySet()) {
                String fromKey = entry.getKey();
                RuleNode fromNode = graph.getNode(fromKey);
                int fromObs = fromNode != null ? fromNode.getObservationCount() : 0;
                for (DependencyEdge edge : entry.getValue()) {
                    w.write(String.format("%s,%s,%d,%f,%f",
                            escapeCsv(edge.getFromNodeKey()),
                            escapeCsv(edge.getToNodeKey()),
                            edge.getFireCount(),
                            edge.getProbability(fromObs),
                            edge.getAvgBenefit()));
                    w.newLine();
                }
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

