package org.autorewriter.graph.io;

import org.autorewriter.graph.model.DependencyEdge;
import org.autorewriter.graph.model.RuleDependencyGraph;
import org.autorewriter.graph.model.RuleNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GraphExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void testExportCreatesTwoFiles() throws Exception {
        RuleDependencyGraph graph = new RuleDependencyGraph(
                Map.of(0, new RuleNode(0, "Filter-LeftJoin", "InnerJoin", 10),
                       1, new RuleNode(1, "InnerJoin", "Project", 7)),
                Map.of(0, List.of(new DependencyEdge(0, 1, 7, 8750.0))));

        new GraphExporter().exportForGNN(graph, tempDir);

        assertTrue(Files.exists(tempDir.resolve("nodes.csv")));
        assertTrue(Files.exists(tempDir.resolve("edges.csv")));
    }

    @Test
    void testNodesCsvHeader() throws Exception {
        RuleDependencyGraph graph = new RuleDependencyGraph(
                Map.of(0, new RuleNode(0, "Filter", "Project", 5)), Map.of());

        new GraphExporter().exportForGNN(graph, tempDir);

        String header = Files.readAllLines(tempDir.resolve("nodes.csv")).get(0);
        assertEquals("ruleId,sourceSignature,targetSignature,observationCount,fireRate", header);
    }

    @Test
    void testEdgesCsvHeader() throws Exception {
        new GraphExporter().exportForGNN(new RuleDependencyGraph(), tempDir);

        String header = Files.readAllLines(tempDir.resolve("edges.csv")).get(0);
        assertEquals("fromRuleId,toRuleId,fireCount,probability,avgBenefit", header);
    }

    @Test
    void testNodesCsvDataRow() throws Exception {
        RuleDependencyGraph graph = new RuleDependencyGraph(
                Map.of(0, new RuleNode(0, "Filter-LeftJoin", "InnerJoin", 10)), Map.of());

        new GraphExporter().exportForGNN(graph, tempDir);

        List<String> lines = Files.readAllLines(tempDir.resolve("nodes.csv"));
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).startsWith("0,Filter-LeftJoin,InnerJoin,10,"));
    }

    @Test
    void testEdgesCsvDataRow() throws Exception {
        RuleDependencyGraph graph = new RuleDependencyGraph(
                Map.of(0, new RuleNode(0, "Filter-LeftJoin", "InnerJoin", 10),
                       1, new RuleNode(1, "InnerJoin", "Project", 7)),
                Map.of(0, List.of(new DependencyEdge(0, 1, 7, 8750.0))));

        new GraphExporter().exportForGNN(graph, tempDir);

        List<String> lines = Files.readAllLines(tempDir.resolve("edges.csv"));
        assertEquals(2, lines.size());
        // probability = 7/10 = 0.7, avgBenefit = 8750/7 = 1250.0
        String row = lines.get(1);
        assertTrue(row.startsWith("0,1,7,"), "Row should start with fromId,toId,fireCount: " + row);
        assertTrue(row.contains("0.700000"), "Row should contain probability 0.7: " + row);
        assertTrue(row.contains("1250.000000"), "Row should contain avgBenefit 1250.0: " + row);
    }
}
