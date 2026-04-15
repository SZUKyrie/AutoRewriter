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

    @Test
    void testRoundTrip() throws Exception {
        RuleNode nodeA = new RuleNode(0, "Filter-LeftJoin-Input-Input", "InnerJoin-Input-Input", 10);
        RuleNode nodeB = new RuleNode(1, "InnerJoin-Input-Input", "Project-InnerJoin-Input-Input", 7);
        DependencyEdge edge = new DependencyEdge(0, 1, 7, 8750.0);

        RuleDependencyGraph original = new RuleDependencyGraph(
                Map.of(0, nodeA, 1, nodeB),
                Map.of(0, List.of(edge)));

        GraphSerializer serializer = new GraphSerializer();
        Path file = tempDir.resolve("graph.json");

        serializer.serialize(original, file);
        assertTrue(file.toFile().exists());

        RuleDependencyGraph restored = serializer.deserialize(file);

        assertEquals(2, restored.nodeCount());
        assertEquals(1, restored.edgeCount());
        assertEquals("Filter-LeftJoin-Input-Input", restored.getNode(0).getSourceTemplateSignature());
        assertEquals(10, restored.getNode(0).getObservationCount());
        assertEquals(7, restored.getOutEdges(0).get(0).getFireCount());
        assertEquals(8750.0 / 7, restored.getOutEdges(0).get(0).getAvgBenefit(), 1e-6);
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
