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

    private static RuleNode node(int ruleId, String srcSig, String tgtSig, String matchedSig, int obs) {
        return new RuleNode(RuleNode.keyOf(ruleId, matchedSig), ruleId, srcSig, tgtSig, matchedSig, obs);
    }

    private static DependencyEdge edge(int fromId, String fromSig, int toId, String toSig,
                                        int count, double benefit) {
        return new DependencyEdge(
                RuleNode.keyOf(fromId, fromSig), RuleNode.keyOf(toId, toSig), count, benefit);
    }

    @Test
    void testExportCreatesTwoFiles() throws Exception {
        RuleNode na = node(0, "Filter-LeftJoin", "InnerJoin", "Filter-LeftJoin", 10);
        RuleNode nb = node(1, "InnerJoin", "Project", "InnerJoin", 7);
        DependencyEdge e = edge(0, "Filter-LeftJoin", 1, "InnerJoin", 7, 8750.0);

        RuleDependencyGraph graph = new RuleDependencyGraph(
                Map.of(na.getNodeKey(), na, nb.getNodeKey(), nb),
                Map.of(na.getNodeKey(), List.of(e)));

        new GraphExporter().exportForGNN(graph, tempDir);

        assertTrue(Files.exists(tempDir.resolve("nodes.csv")));
        assertTrue(Files.exists(tempDir.resolve("edges.csv")));
    }

    @Test
    void testNodesCsvHeader() throws Exception {
        RuleNode na = node(0, "Filter", "Project", "Filter", 5);
        new GraphExporter().exportForGNN(
                new RuleDependencyGraph(Map.of(na.getNodeKey(), na), Map.of()),
                tempDir);

        String header = Files.readAllLines(tempDir.resolve("nodes.csv")).get(0);
        assertEquals("nodeKey,ruleId,sourceSignature,targetSignature,matchedNodeSignature,observationCount,fireRate",
                header);
    }

    @Test
    void testEdgesCsvHeader() throws Exception {
        new GraphExporter().exportForGNN(new RuleDependencyGraph(), tempDir);

        String header = Files.readAllLines(tempDir.resolve("edges.csv")).get(0);
        assertEquals("fromNodeKey,toNodeKey,fireCount,probability,avgBenefit", header);
    }

    @Test
    void testNodesCsvDataRow() throws Exception {
        RuleNode na = node(0, "Filter-LeftJoin", "InnerJoin", "Filter-LeftJoin", 10);
        new GraphExporter().exportForGNN(
                new RuleDependencyGraph(Map.of(na.getNodeKey(), na), Map.of()),
                tempDir);

        List<String> lines = Files.readAllLines(tempDir.resolve("nodes.csv"));
        assertEquals(2, lines.size());
        // nodeKey = "0:Filter-LeftJoin", ruleId=0
        assertTrue(lines.get(1).contains("0"), "Row should contain ruleId 0: " + lines.get(1));
        assertTrue(lines.get(1).contains("Filter-LeftJoin"), "Row should contain signature: " + lines.get(1));
    }

    @Test
    void testEdgesCsvDataRow() throws Exception {
        RuleNode na = node(0, "Filter-LeftJoin", "InnerJoin", "Filter-LeftJoin", 10);
        RuleNode nb = node(1, "InnerJoin", "Project", "InnerJoin", 7);
        DependencyEdge e = edge(0, "Filter-LeftJoin", 1, "InnerJoin", 7, 8750.0);

        RuleDependencyGraph graph = new RuleDependencyGraph(
                Map.of(na.getNodeKey(), na, nb.getNodeKey(), nb),
                Map.of(na.getNodeKey(), List.of(e)));

        new GraphExporter().exportForGNN(graph, tempDir);

        List<String> lines = Files.readAllLines(tempDir.resolve("edges.csv"));
        assertEquals(2, lines.size());
        // probability = 7/10 = 0.7, avgBenefit = 8750/7 = 1250.0
        String row = lines.get(1);
        assertTrue(row.contains("0.700000"), "Row should contain probability 0.7: " + row);
        assertTrue(row.contains("1250.000000"), "Row should contain avgBenefit 1250.0: " + row);
    }
}
