package e2e;

import org.autorewriter.e2e.RewritePathE2ETest;
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
        runWithGraph(outputDir, "diaspora", RULE_DIR, PipelineType.CBO);
    }

    @Test
    public void testDiasporaRboFullRules() {
        Path outputDir = Paths.get("target", "graph-output");
        runWithGraph(outputDir, "diaspora", RULE_DIR, PipelineType.MANUAL);
    }
}

