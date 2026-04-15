package org.autorewriter.graph.io;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.autorewriter.graph.model.DependencyEdge;
import org.autorewriter.graph.model.RuleDependencyGraph;
import org.autorewriter.graph.model.RuleNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Serializes and deserializes {@link RuleDependencyGraph} to/from JSON on disk.
 * Returns an empty graph if the file is missing or corrupted.
 */
@Slf4j
public class GraphSerializer {

    private final ObjectMapper mapper;

    public GraphSerializer() {
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /** Serialize graph to JSON file. Parent directories are created if needed. */
    public void serialize(RuleDependencyGraph graph, Path outputPath) throws IOException {
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        mapper.writeValue(outputPath.toFile(), new GraphDto(graph));
        log.info("Graph serialized to {} ({} nodes, {} edges)",
                outputPath, graph.nodeCount(), graph.edgeCount());
    }

    /** Deserialize graph from JSON file. Returns empty graph if file missing or unreadable. */
    public RuleDependencyGraph deserialize(Path inputPath) {
        if (!Files.exists(inputPath)) {
            log.debug("Graph file not found at {}, returning empty graph", inputPath);
            return new RuleDependencyGraph();
        }
        try {
            GraphDto dto = mapper.readValue(inputPath.toFile(), GraphDto.class);
            return dto.toGraph();
        } catch (Exception e) {
            log.warn("Failed to deserialize graph from {}, returning empty graph", inputPath, e);
            return new RuleDependencyGraph();
        }
    }

    // ── DTO ───────────────────────────────────────────────────────────────

    static class GraphDto {
        @JsonProperty("version")
        public int version = 1;

        @JsonProperty("nodes")
        public Map<Integer, RuleNode> nodes;

        @JsonProperty("outEdges")
        public Map<Integer, List<DependencyEdge>> outEdges;

        GraphDto() {}

        GraphDto(RuleDependencyGraph graph) {
            this.nodes    = graph.getNodes();
            this.outEdges = graph.getOutEdges();
        }

        RuleDependencyGraph toGraph() {
            return new RuleDependencyGraph(nodes, outEdges);
        }
    }
}
