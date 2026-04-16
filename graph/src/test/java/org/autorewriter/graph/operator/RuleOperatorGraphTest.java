package org.autorewriter.graph.operator;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.autorewriter.rewriter.analyze.RuleAnalysisContext;
import org.autorewriter.rewriter.analyze.RuleAnalyzer;
import org.autorewriter.rewriter.rule.AutoRewriteRule;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 读取规则文件中所有规则，构建 RuleOperatorGraph 并导出 PNG。
 */
@Slf4j
class RuleOperatorGraphTest {

    /**
     * 规则资源目录（与 E2E 测试保持一致，指向 test 模块的 src/main/resources/example）。
     */
    private static final String RULE_DIR = resolveRuleDir();

    /** PNG 输出目录（graph 模块的 target/graph-output/） */
    private static final Path OUTPUT_DIR = Paths.get("target", "graph-output");

    @Test
    void testBuildAndExportOperatorGraph() throws Exception {
        // ── 1. 读取规则 ─────────────────────────────────────────────────
        List<RuleAnalysisContext> contexts = readRuleContexts(RULE_DIR);
        log.info("Loaded {} rule contexts from {}", contexts.size(), RULE_DIR);
        assertTrue(contexts.size() > 0, "应至少解析出 1 条规则");

        // ── 2. 构建图 ────────────────────────────────────────────────────
        RuleOperatorGraphBuilder builder = new RuleOperatorGraphBuilder();
        for (int i = 0; i < contexts.size(); i++) {
            RuleAnalysisContext ctx = contexts.get(i);
            if (ctx.isNoOp()) continue;   // 跳过等价变换（源==目标）

            // 构造一个轻量 AutoRewriteRule（仅需 sourceTemplate / targetTemplate / ruleId）
            AutoRewriteRule rule = new AutoRewriteRule(
                    RelOptRule.operand(ctx.getSourceRelNode().getClass(), RelOptRule.any()),
                    ctx,
                    i
            );
            builder.addRule(rule);
        }

        RuleOperatorGraph graph = builder.build();
        log.info("RuleOperatorGraph: {} nodes, {} edges ({} structural, {} transform)",
                graph.nodeCount(),
                graph.edgeCount(),
                graph.structuralEdges().size(),
                graph.transformEdges().size());

        assertTrue(graph.nodeCount() > 0, "图中应有节点");
        assertTrue(graph.transformEdges().size() > 0, "图中应有转换边");

        // ── 3. 导出 DOT ──────────────────────────────────────────────────
        Files.createDirectories(OUTPUT_DIR);
        Path dotPath = OUTPUT_DIR.resolve("rule-operator-graph.dot");
        RuleOperatorGraphVisualizer.exportToDot(graph, dotPath);
        log.info("DOT exported to {}", dotPath.toAbsolutePath());
        assertTrue(Files.exists(dotPath));

        // ── 4. 导出 SVG 矢量图（可任意缩放，文字清晰）─────────────────────
        Path svgPath = OUTPUT_DIR.resolve("rule-operator-graph.svg");
        try {
            RuleOperatorGraphVisualizer.exportToSvg(graph, svgPath);
            log.info("SVG exported to {}", svgPath.toAbsolutePath());
            assertTrue(Files.exists(svgPath));
        } catch (Exception e) {
            log.warn("SVG export failed (Graphviz may not be installed): {}", e.getMessage());
        }

        // ── 5. 导出 PNG（备用）──────────────────────────────────────────
        Path pngPath = OUTPUT_DIR.resolve("rule-operator-graph.png");
        try {
            RuleOperatorGraphVisualizer.exportToPng(graph, pngPath);
            log.info("PNG exported to {}", pngPath.toAbsolutePath());
        } catch (Exception e) {
            log.warn("PNG export failed: {}", e.getMessage());
        }
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    private static List<RuleAnalysisContext> readRuleContexts(String ruleDir) {
        List<RuleAnalysisContext> contexts = new ArrayList<>();
        try {
            List<Path> ruleFiles = Files.list(Paths.get(ruleDir))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());

            for (Path ruleFile : ruleFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(ruleFile.toFile()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        try {
                            RuleAnalysisContext ctx = RuleAnalyzer.analyze(line);
                            if (ctx != null
                                    && ctx.getSourceRelNode() != null
                                    && ctx.getTargetRelNode() != null) {
                                contexts.add(ctx);
                            }
                        } catch (Exception e) {
                            log.debug("Skip unparseable rule: {}", line.substring(0, Math.min(60, line.length())));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read rule files from " + ruleDir, e);
        }
        return contexts;
    }

    /**
     * 动态解析规则文件所在目录（指向 test 模块 resources/example）。
     * 通过当前类文件往上寻找含有 "test" 子模块的 AutoRewriter 根，再拼接路径。
     */
    private static String resolveRuleDir() {
        // graph 模块运行时，test 模块资源不在 classpath，直接用相对路径定位
        Path candidate = Paths.get("../test/src/main/resources/example").toAbsolutePath();
        if (Files.isDirectory(candidate)) {
            return candidate.toString();
        }
        // 兜底：从当前工作目录找
        Path fallback = Paths.get("").toAbsolutePath();
        // 向上找到包含 test/ 的根目录
        Path root = fallback;
        while (root != null && !Files.isDirectory(root.resolve("test"))) {
            root = root.getParent();
        }
        if (root != null) {
            return root.resolve("test/src/main/resources/example").toString();
        }
        throw new IllegalStateException("Cannot locate test/src/main/resources/example");
    }
}
