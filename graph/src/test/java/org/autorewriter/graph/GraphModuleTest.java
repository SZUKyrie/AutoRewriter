package org.autorewriter.graph;

import org.autorewriter.rewriter.optimize.trace.OptimizationTrace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraphModuleTest {

    @TempDir
    Path tempDir;

    @Test
    void testFlushWritesJsonFile() throws Exception {
        Path persistPath = tempDir.resolve("graph.json");
        GraphModule module = GraphModule.load(persistPath, 0);

        module.record(new OptimizationTrace());
        module.flush();

        assertTrue(Files.exists(persistPath));
    }

    @Test
    void testAutoFlushAfterInterval() throws Exception {
        Path persistPath = tempDir.resolve("graph.json");
        GraphModule module = GraphModule.load(persistPath, 2);

        module.record(new OptimizationTrace());
        assertFalse(Files.exists(persistPath), "Should not flush after 1 record (interval=2)");

        module.record(new OptimizationTrace());
        assertTrue(Files.exists(persistPath), "Should auto-flush after 2nd record");
    }

    @Test
    void testLoadRestoresExistingGraph() throws Exception {
        Path persistPath = tempDir.resolve("graph.json");

        GraphModule module1 = GraphModule.load(persistPath, 0);
        module1.flush();
        assertTrue(Files.exists(persistPath));

        // Second load should not throw
        GraphModule module2 = GraphModule.load(persistPath, 1);
        assertNotNull(module2);
    }

    @Test
    void testRankRulesReturnsAllCandidatesWhenNoHistory() {
        Path persistPath = tempDir.resolve("graph.json");
        GraphModule module = GraphModule.load(persistPath, 1);

        List<Integer> ranked = module.rankRules(List.of(0, 1, 2), -1);
        assertEquals(3, ranked.size());
    }

    @Test
    void testExportCreatesCsvFiles() throws Exception {
        Path persistPath = tempDir.resolve("graph.json");
        Path exportDir   = tempDir.resolve("export");

        GraphModule module = GraphModule.load(persistPath, 1);
        module.export(exportDir);

        assertTrue(Files.exists(exportDir.resolve("nodes.csv")));
        assertTrue(Files.exists(exportDir.resolve("edges.csv")));
    }
}
