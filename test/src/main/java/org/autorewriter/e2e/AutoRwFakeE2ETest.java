package org.autorewriter.e2e;

import org.junit.Test;

public class
AutoRwFakeE2ETest extends AutoRwFakeE2ETesBase{
//    @Test
//    public void testTpcds() {
//        executePipeline(PipelineType.MANUAL, "tpcds", RULE_DIR);
//    }
//
//    @Test
//    public void testDiapora() {
//        executePipeline(PipelineType.MANUAL, "diaspora", RULE_DIR);
//    }
//    @Test
//    public void testTpcdsCbo() {
//        executePipeline(PipelineType.CBO, "tpcds", RULE_DIR);
//    }
//
    @Test
    public void testDiasporaCbo() {
        executePipeline(PipelineType.CBO, "diaspora", RULE_DIR);
    }
}
