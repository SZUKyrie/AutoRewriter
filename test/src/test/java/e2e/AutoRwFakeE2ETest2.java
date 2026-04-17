package e2e;

import org.autorewriter.e2e.RewritePathE2ETest;
import org.autorewriter.graph.GraphModule;
import org.autorewriter.graph.model.GraphSummary;
import org.autorewriter.graph.model.RuleDependencyGraph;
import org.autorewriter.rewriter.pipleline.costbase.CostBaseProducePipeline;
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
        Path outputDir = Paths.get("target", "graph-output");

        GraphModule graphModule = GraphModule.load(
                outputDir.resolve("rule-dependency-graph.json"), 0);

        org.autorewriter.rewriter.pipleline.ProduceContext context =
                createContextPublic("diaspora", RULE_DIR);

        new CostBaseProducePipeline()
                .withTraceConsumer(graphModule)
                .run(context);

        graphModule.export(outputDir.resolve("gnn-input"));
        graphModule.visualizeDot(outputDir.resolve("rule-dependency-graph.dot"));
        graphModule.visualize(outputDir.resolve("rule-dependency-graph.png"));

        RuleDependencyGraph graph = graphModule.buildGraph();
        System.out.printf("[Graph] %d nodes, %d edges → %s%n",
                graph.nodeCount(), graph.edgeCount(),
                outputDir.toAbsolutePath());

        String summary = new GraphSummary(graph).report(30);
        System.out.println(summary);
    }

    @Test
    public void testDiaporaRboFullRules() {
        executePipeline(PipelineType.MANUAL, "diaspora", RULE_DIR);
    }
}

