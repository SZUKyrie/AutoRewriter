package org.autorewriter.rewriter.optimize.ruleBaseOpt;

import org.autorewriter.rewriter.analyze.RuleAnalysisContext;
import org.autorewriter.rewriter.analyze.RuleAnalyzer;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RuleParserSuccessRateTest {

    private static final String RULE_FILES_DIR = "/Users/yuekunyu/PersonalProject/AutoRewriter/test/src/main/resources/example";
    private static final String SEPARATOR = "─".repeat(80);
    private static final String DOUBLE_SEPARATOR = "═".repeat(80);

    @Test
    public void testRuleParsingSuccessRate() {
        System.out.println("\n" + DOUBLE_SEPARATOR);
        System.out.println("规则文件解析成功率测试");
        System.out.println(DOUBLE_SEPARATOR);

        File directory = new File(RULE_FILES_DIR);
        if (!directory.exists() || !directory.isDirectory()) {
            fail("规则文件目录不存在: " + RULE_FILES_DIR);
        }

        File[] ruleFiles = directory.listFiles((dir, name) -> name.endsWith(".txt"));
        if (ruleFiles == null || ruleFiles.length == 0) {
            fail("未找到任何规则文件");
        }

        int totalFiles = ruleFiles.length;
        int totalRules = 0;
        int successfulRules = 0;
        int failedRules = 0;

        Map<String, FileParseResult> fileResults = new HashMap<>();

        for (File ruleFile : ruleFiles) {
            System.out.println("\n" + SEPARATOR);
            System.out.println("正在解析文件: " + ruleFile.getName());
            System.out.println(SEPARATOR);

            FileParseResult result = parseRuleFile(ruleFile);
            fileResults.put(ruleFile.getName(), result);

            totalRules += result.getTotalRules();
            successfulRules += result.getSuccessCount();
            failedRules += result.getFailCount();

            System.out.println("文件规则总数: " + result.getTotalRules());
            System.out.println("解析成功: " + result.getSuccessCount());
            System.out.println("解析失败: " + result.getFailCount());
            System.out.println("成功率: " + String.format("%.2f%%", result.getSuccessRate()));

            if (!result.getFailedRules().isEmpty()) {
                Map<String, Integer> errorTypes = new HashMap<>();
                for (FailedRule failedRule : result.getFailedRules()) {
                    String errorType = extractErrorType(failedRule.getErrorMessage());
                    errorTypes.put(errorType, errorTypes.getOrDefault(errorType, 0) + 1);
                }

                System.out.println("\n错误类型统计:");
                for (Map.Entry<String, Integer> entry : errorTypes.entrySet()) {
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue());
                }

                System.out.println("\n失败的规则 (前5条):");
                int count = 0;
                for (FailedRule failedRule : result.getFailedRules()) {
                    if (count++ >= 5) break;
                    System.out.println("  行 " + failedRule.getLineNumber() + ": " +
                        failedRule.getErrorMessage());
                    System.out.println("    完整规则: " + failedRule.getRuleString());
                    System.out.println();
                }
                if (result.getFailedRules().size() > 5) {
                    System.out.println("  ... 还有 " + (result.getFailedRules().size() - 5) + " 条失败规则");
                }
            }
        }

        System.out.println("\n" + DOUBLE_SEPARATOR);
        System.out.println("总体统计");
        System.out.println(DOUBLE_SEPARATOR);
        System.out.println("文件总数: " + totalFiles);
        System.out.println("规则总数: " + totalRules);
        System.out.println("解析成功: " + successfulRules);
        System.out.println("解析失败: " + failedRules);
        double overallSuccessRate = totalRules > 0 ? (successfulRules * 100.0 / totalRules) : 0;
        System.out.println("总体成功率: " + String.format("%.2f%%", overallSuccessRate));
        System.out.println(DOUBLE_SEPARATOR);

        System.out.println("\n" + SEPARATOR);
        System.out.println("各文件详细统计");
        System.out.println(SEPARATOR);
        System.out.printf("%-30s %10s %10s %10s %10s%n",
            "文件名", "规则总数", "成功", "失败", "成功率");
        System.out.println(SEPARATOR);

        for (Map.Entry<String, FileParseResult> entry : fileResults.entrySet()) {
            FileParseResult result = entry.getValue();
            System.out.printf("%-30s %10d %10d %10d %9.2f%%%n",
                entry.getKey(),
                result.getTotalRules(),
                result.getSuccessCount(),
                result.getFailCount(),
                result.getSuccessRate());
        }
        System.out.println(SEPARATOR);

        assertTrue(overallSuccessRate > 0, "至少应该有一些规则能够成功解析");
    }

    private FileParseResult parseRuleFile(File file) {
        List<String> rules = new ArrayList<>();
        List<FailedRule> failedRules = new ArrayList<>();
        int successCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                rules.add(line);

                try {
                    RuleAnalysisContext context = RuleAnalyzer.analyze(line);
                    if (context != null && context.getSourceRelNode() != null &&
                        context.getTargetRelNode() != null) {
                        successCount++;
                    } else {
                        failedRules.add(new FailedRule(lineNumber, line, "解析结果为空"));
                    }
                } catch (AssertionError e) {
                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = e.getClass().getSimpleName();
                    }
                    failedRules.add(new FailedRule(lineNumber, line, "断言错误: " + errorMsg));
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = e.getClass().getSimpleName();
                    }
                    if (e.getCause() != null && e.getCause().getMessage() != null) {
                        errorMsg = errorMsg + " (Cause: " + e.getCause().getMessage() + ")";
                    }
                    failedRules.add(new FailedRule(lineNumber, line, errorMsg));
                } catch (Error e) {
                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = e.getClass().getSimpleName();
                    }
                    failedRules.add(new FailedRule(lineNumber, line, "严重错误: " + errorMsg));
                }
            }
        } catch (IOException e) {
            System.err.println("读取文件失败: " + file.getName() + " - " + e.getMessage());
        }

        return new FileParseResult(rules.size(), successCount, failedRules);
    }

    private String extractErrorType(String errorMsg) {
        if (errorMsg.contains("RexInputRef index") && errorMsg.contains("out of range")) {
            return "列索引超出范围";
        } else if (errorMsg.contains("断言错误")) {
            return "断言错误";
        } else if (errorMsg.contains("NullPointerException")) {
            return "空指针异常";
        } else if (errorMsg.contains("ClassCastException")) {
            return "类型转换异常";
        } else if (errorMsg.contains("IllegalArgumentException")) {
            return "非法参数异常";
        } else if (errorMsg.contains("严重错误")) {
            return "严重错误";
        } else if (errorMsg.contains("解析结果为空")) {
            return "解析结果为空";
        } else {
            return "其他错误";
        }
    }

    private static class FileParseResult {
        private final int totalRules;
        private final int successCount;
        private final List<FailedRule> failedRules;

        public FileParseResult(int totalRules, int successCount, List<FailedRule> failedRules) {
            this.totalRules = totalRules;
            this.successCount = successCount;
            this.failedRules = failedRules;
        }

        public int getTotalRules() {
            return totalRules;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailCount() {
            return failedRules.size();
        }

        public List<FailedRule> getFailedRules() {
            return failedRules;
        }

        public double getSuccessRate() {
            return totalRules > 0 ? (successCount * 100.0 / totalRules) : 0;
        }
    }

    private static class FailedRule {
        private final int lineNumber;
        private final String ruleString;
        private final String errorMessage;

        public FailedRule(int lineNumber, String ruleString, String errorMessage) {
            this.lineNumber = lineNumber;
            this.ruleString = ruleString;
            this.errorMessage = errorMessage;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getRuleString() {
            return ruleString;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

