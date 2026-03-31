package e2e;

import org.autorewriter.e2e.RewritePathE2ETest;
import org.junit.Test;

public class AutoRwFakeE2ETest2 extends RewritePathE2ETest {
    @Test
    public void testDiaspora2Cbo() {
        executePipeline(PipelineType.CBO, "diaspora", RULE_DIR3);
    }

    @Test
    public void testDiapora2() {
        executePipeline(PipelineType.MANUAL, "diaspora", RULE_DIR3);
    }

    @Test
    public void testDiasporaCboFullRules() {
        executePipeline(PipelineType.CBO, "diaspora", RULE_DIR);
    }
}
