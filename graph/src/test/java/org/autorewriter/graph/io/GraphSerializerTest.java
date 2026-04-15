package org.autorewriter.graph.io;

import org.autorewriter.graph.model.DependencyEdge;
import org.autorewriter.graph.model.RuleDependencyGraph;
import org.autorewriter.graph.model.RuleNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GraphSerializerTest {

    @TempDir
    Path tempDir;

    private static RuleNode node(int ruleId, String srcSig, String tgtSig, String matchedSig, int obs) {
        return new RuleNode(RuleNode.keyOf(ruleId, matchedSig), ruleId, srcSig, tgtSig, matchedSig, obs);
    }

    private static DependencyEdge edge(int fromId, String fromMatchedSig,
                                       int toId,   String toMatchedSig,
                                       int count, double benefit) {
        return new DependencyEdge(
                RuleNode.keyOf(fromId, fromMatchedSig),
                RuleNode.keyOf(toId,   toMatchedSig),
                count, benefit);
    }

    @Test
    void testRoundTrip() throws Exception {
        RuleNode nodeA = node(0, "Filter-LeftJoin", "InnerJoin", "Filter-LeftJoin", 10);
        RuleNode nodeB = node(1, "InnerJoin",       "Project",   "InnerJoin",       7);
        DependencyEdge e = edge(0, "Filter-LeftJoin", 1, "InnerJoin", 7, 8750.0);

        RuleDependencyGraph original = new RuleDependencyGraph(
                Map.of(nodeA.getNodeKey(), nodeA, nodeB.getNodeKey(), nodeB),
                Map.of(nodeA.getNodeKey(), List.of(e)));

        GraphSerializer serializer = new GraphSerializer();
        Path file = tempDir.resolve("graph.json");
        serializer.serialize(original, file);
        assertTrue(file.toFile().exists());

        RuleDependencyGraph restored = serializer.deserialize(file);
        assertEquals(2, restored.nodeCount());
        assertEquals(1, restored.edgeCount());

        String keyA = RuleNode.keyOf(0, "Filter-LeftJoin");
        assertEquals("Filter-LeftJoin", restored.getNode(keyA).getSourceTemplateSignature());
        assertEquals(10, restored.getNode(keyA).getObservationCount());
        assertEquals(7, restored.getOutEdges(keyA).get(0).getFireCount());
        assertEquals(8750.0 / 7, restored.getOutEdges(keyA).get(0).getAvgBenefit(), 1e-6);
    }

    @Test
    void testDeserializeNonExistentFileReturnsEmptyGraph() {
        GraphSerializer serializer = new GraphSerializer();
        Path missing = tempDir.resolve("nonexistent.json");

        RuleDependencyGraph graph = serializer.deserialize(missing);
        assertEquals(0, graph.nodeCount());
        assertEquals(0, graph.edgeCount());
    }
}
