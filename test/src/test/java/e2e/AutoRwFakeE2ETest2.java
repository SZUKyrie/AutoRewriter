package e2e;

import org.autorewriter.e2e.RewritePathE2ETest;
import org.junit.Test;

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
        executePipeline(PipelineType.CBO, "diaspora", RULE_DIR);
    }

    @Test
    public void testDiaporaRboFullRules() {
        executePipeline(PipelineType.MANUAL, "diaspora", RULE_DIR);
    }
}
