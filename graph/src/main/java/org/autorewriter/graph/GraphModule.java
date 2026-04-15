package org.autorewriter.graph;

import lombok.extern.slf4j.Slf4j;
import org.autorewriter.graph.builder.RuleGraphBuilder;
import org.autorewriter.graph.io.GraphExporter;
import org.autorewriter.graph.io.GraphSerializer;
import org.autorewriter.graph.io.GraphVisualizer;
import org.autorewriter.graph.model.DependencyEdge;
import org.autorewriter.graph.model.RuleDependencyGraph;
import org.autorewriter.rewriter.optimize.trace.OptimizationTrace;
import org.autorewriter.rewriter.optimize.trace.TraceConsumer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Facade for the graph module.
 *
 * <p>Usage:
 * <pre>
 *   GraphModule module = GraphModule.load(Path.of("graph.json"), 100);
 *   module.record(trace);   // in optimization loop
 *   module.flush();         // persist
 *   module.export(Path.of("gnn-input/"));  // export for Python
 * </pre>
 */
@Slf4j
public class GraphModule implements TraceConsumer {

    private final RuleGraphBuilder builder;
    private final GraphSerializer  serializer;
    private final GraphExporter    exporter;
    private final Path             persistPath;
    private final int              flushInterval;
    private       int              recordCount;

    private GraphModule(RuleGraphBuilder builder,
                        GraphSerializer  serializer,
                        GraphExporter    exporter,
                        Path             persistPath,
                        int              flushInterval) {
        this.builder       = builder;
        this.serializer    = serializer;
        this.exporter      = exporter;
        this.persistPath   = persistPath;
        this.flushInterval = flushInterval;
        this.recordCount   = 0;
    }

    /**
     * Load (or create) a GraphModule. Restores persisted state if the file exists.
     *
     * @param persistPath  JSON persistence file path (need not exist yet)
     * @param flushInterval auto-flush every N records; 0 = flush on every record
     */
    public static GraphModule load(Path persistPath, int flushInterval) {
        GraphSerializer  serializer = new GraphSerializer();
        RuleGraphBuilder builder    = new RuleGraphBuilder();

        RuleDependencyGraph existing = serializer.deserialize(persistPath);
        if (existing.nodeCount() > 0) {
            log.info("Restored graph from {} ({} nodes, {} edges)",
                    persistPath, existing.nodeCount(), existing.edgeCount());
        }

        return new GraphModule(builder, serializer, new GraphExporter(), persistPath, flushInterval);
    }

    /**
     * Record one query's OptimizationTrace. Auto-flushes if the interval is reached.
     */
    public void record(OptimizationTrace trace) {
        builder.record(trace);
        recordCount++;
        if (flushInterval == 0 || recordCount % flushInterval == 0) {
            flush();
        }
    }

    /**
     * Implements {@link TraceConsumer}: delegates to {@link #record(OptimizationTrace)}.
     * Allows this module to be injected directly into {@code CostBaseProducePipeline}.
     */
    @Override
    public void consume(OptimizationTrace trace) {
        record(trace);
    }

    /** Build and return the current in-memory graph snapshot. */
    public RuleDependencyGraph buildGraph() {
        return builder.build();
    }

    /** Persist the current graph state to disk. */
    public void flush() {
        try {
            serializer.serialize(builder.build(), persistPath);
        } catch (IOException e) {
            log.error("Failed to flush graph to {}", persistPath, e);
        }
    }

    /** Export graph to CSV files for Python GNN training. */
    public void export(Path outputDir) {
        try {
            exporter.exportForGNN(builder.build(), outputDir);
        } catch (IOException e) {
            log.error("Failed to export graph to {}", outputDir, e);
        }
    }

    /** Export the graph as a PNG visualization (requires Graphviz dot command). */
    public void visualize(Path outputPath) {
        try {
            GraphVisualizer.exportToPng(builder.build(), outputPath);
        } catch (IOException e) {
            log.error("Failed to visualize graph to {}", outputPath, e);
        }
    }

    /** Export the graph as a DOT file (no external dependencies required). */
    public void visualizeDot(Path outputPath) {
        try {
            GraphVisualizer.exportToDot(builder.build(), outputPath);
        } catch (IOException e) {
            log.error("Failed to export DOT to {}", outputPath, e);
        }
    }

    /**
     * Rank candidate node keys by observed transition probability from currentNodeKey.
     * Nodes with no history are appended at the end in original order.
     *
     * @param candidateNodeKeys node keys to rank (format: "ruleId:matchedSig")
     * @param currentNodeKey    the node key that just fired (null if none)
     */
    public List<String> rankRules(List<String> candidateNodeKeys, String currentNodeKey) {
        RuleDependencyGraph graph = builder.build();
        List<DependencyEdge> outEdges = currentNodeKey != null
                ? graph.getOutEdges(currentNodeKey)
                : Collections.emptyList();

        Map<String, Double> probMap = new HashMap<>();
        int fromObs = currentNodeKey != null && graph.getNode(currentNodeKey) != null
                ? graph.getNode(currentNodeKey).getObservationCount() : 0;
        for (DependencyEdge e : outEdges) {
            probMap.put(e.getToNodeKey(), e.getProbability(fromObs));
        }

        List<String> ranked = new ArrayList<>(candidateNodeKeys);
        ranked.sort(Comparator.comparingDouble(
                (String key) -> probMap.getOrDefault(key, 0.0)).reversed());
        return ranked;
    }
}
