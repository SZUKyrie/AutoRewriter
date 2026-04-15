package e2e;

import org.autorewriter.e2e.RewritePathE2ETest;
import org.autorewriter.graph.GraphModule;
import org.autorewriter.rewriter.pipleline.costbase.CostBaseProducePipeline;
import org.autorewriter.rewriter.pipleline.result.ProduceResult;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AutoRwFakeE2ETest2 extends RewritePathE2ETest {

    @Test
    public void testDiasporaCboTiny() {
        executePipeline(PipelineType.CBO, "diaspora", RULE_DIR3);
    }

    @Test
    public void testDiapora2RboTiny() {
        executePipeline(PipelineType.MANUAL, "diaspora", RULE_DIR3);
    }

    @Test
    public void testDiasporaCboFullRules() {
        // Output directory: target/graph-output/
        Path outputDir = Paths.get("target", "graph-output");
        GraphModule graphModule = GraphModule.load(outputDir.resolve("rule-dependency-graph.json"), 0);

        // Build context & run pipeline with GraphModule wired in
        org.autorewriter.rewriter.pipleline.ProduceContext context =
                createContextPublic("diaspora", RULE_DIR);

        CostBaseProducePipeline pipeline = new CostBaseProducePipeline()
                .withTraceConsumer(graphModule);
        pipeline.run(context);

        // Export CSV for Python GNN
        graphModule.export(outputDir.resolve("gnn-input"));

        // Export DOT (no external dependency needed)
        graphModule.visualizeDot(outputDir.resolve("rule-dependency-graph.dot"));

        // Export PNG if Graphviz dot is available (best-effort)
        graphModule.visualize(outputDir.resolve("rule-dependency-graph.png"));

        System.out.printf("[Graph] %s nodes, %s edges → %s%n",
                graphModule.rankRules(java.util.List.of(), -1).size(),
                0,
                outputDir.toAbsolutePath());
    }

    @Test
    public void testDiaporaRboFullRules() {
        executePipeline(PipelineType.MANUAL, "diaspora", RULE_DIR);
    }
}
