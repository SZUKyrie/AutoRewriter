# WeTune 测试迁移到 AutoRewriter — 设计文档

**日期：** 2026-04-18  
**状态：** 待审阅

---

## 背景与目标

WeTune 是一个 SQL 重写规则合成系统，包含：
- 650 条重写规则（`wtune_data/prepared/rules.txt`），格式与 AutoRewriter 完全一致
- 8574 条来自 21 个真实应用的 SQL 查询（存于 `wtune_data/wtune.db`）
- 25 个真实应用 schema（存于 `wtune_data/schemas/*.base.schema.sql`）

**目标：** 将 WeTune 的查询和规则迁移到 AutoRewriter，并通过类似 `RewritePathE2ETest` 的端到端测试，验证对于给定查询集合，AutoRewriter 的 RBO 优化器能触发与 WeTune 相同的规则序列（路径）。

---

## 核心概念映射

| WeTune 概念 | AutoRewriter 对应 |
|---|---|
| `Substitution` (rules.txt 行) | `RuleAnalysisContext` (从规则模板解析) |
| `BottomUpOptimizer` 优化路径 | `OptimizationTrace` + `RuleApplicationStep` |
| `OptimizationStep.ruleId()` (行号) | `AutoRewriteRule` 对应的规则行号 |
| `RewriteQuery` TSV 输出 (trace 列) | Excel 预期路径 / JSON trace 文件 |
| `OptimizerTest` 测试用例 | `AutoRwWeTuneE2ETest` |

---

## 数据来源分析

### WeTune 优化路径的获取

WeTune 的 `wtune_opt_stmts` 表当前为空（未预计算）。  
**选择运行 WeTune 的 `RewriteQuery` 生成 trace TSV 文件作为 ground truth**（真正来自 WeTune 优化器的路径）。

**步骤：**
1. 在 WeTune 项目中构建并运行 `RewriteQuery` runner：
   ```bash
   cd /mnt/mydata/yyk/wetune
   mvn install -DskipTests -pl common,stmt,sql,superopt
   mvn exec:java -pl superopt \
     -Dexec.mainClass="wtune.superopt.Main" \
     -Dexec.args="RewriteQuery --app all --dir wtune_data/rewrite"
   ```
2. 输出文件：`wtune_data/rewrite/run{timestamp}/1_query.tsv`
3. TSV 格式：`appName\tstmtId\trewriteIndex\toptimizedSql\t{ruleId1,ruleId2,...}`
   - 其中 `ruleId` = 规则在 `rules.txt` 中的行号（1-based）
4. 将此 TSV 作为 AutoRewriter 验证测试的 ground truth

### 规则 ID 对应关系

WeTune 的规则 ID = `rules.txt` 中的行号（`SubstitutionSupport.loadBank()` 第 `lineNum` 赋值）。  
AutoRewriter 加载同一份 `rules.txt` 时，按相同顺序解析，因此行号天然对应。

---

## 整体架构

```
wetune/wtune_data/schemas/*.base.schema.sql
  └── [脚本迁移] → test/resources/schema/{app}/table_ddl.sql

wetune/wtune_data/wtune.db (wtune_stmts 表)
  └── [脚本导出] → test/resources/schema/{app}/query/q{stmtId}.sql

wetune/wtune_data/prepared/rules.txt
  └── [直接复制] → test/resources/wetune_rules.txt

[首次运行 CaptureRunner]
  └── AutoRewriter RBO 跑每条查询
  └── 捕获每个优化结果的规则序列
  └── 存储为 test/resources/schema/{app}/traces/{stmtId}.json
      格式: {"stmtId": 202, "paths": [{"pathIndex": 0, "ruleLines": [42, 7, 15]}, ...]}

[回归测试 AutoRwWeTuneTraceVerifyTest]
  └── 加载 expected traces JSON
  └── 重跑 RBO
  └── 对比实际 vs expected 规则序列（精确匹配顺序）
  └── 报告 EXACT / SUBSET / MISMATCH / ERROR
```

---

## 详细设计

### 1. 数据迁移脚本

**文件：** `scripts/migrate_wetune_data.py`

功能：
- 从 `wtune.db` 的 `wtune_stmts` 表导出每个 app 的 SQL 查询
- 将 `schemas/*.base.schema.sql` 复制/转换为 AutoRewriter 格式
- 处理方言差异：MySQL app（大多数）使用 backtick，PostgreSQL app（discourse/gitlab/homeland）使用双引号

**输出结构：**
```
test/src/main/resources/schema/
  diaspora/
    table_ddl.sql      ← 来自 diaspora.base.schema.sql
    query/
      q2.sql           ← stmtId=2 的原始 SQL
      q3.sql
      ...
  discourse/
    table_ddl.sql
    query/
      q1.sql
      ...
  ... (21 个 app)
```

**注意事项：**
- 每个 app 单独建目录，避免污染已有的 `diaspora/`（已有 AutoRewriter 自己的测试数据）
- 新 app 目录命名加前缀 `wetune_` 以区分（如 `wetune_diaspora/`），避免与已有 schema 冲突
- 过滤掉单表简单查询（WeTune 的 `isSimple()` 逻辑），只迁移复杂查询（多表 JOIN / 子查询）

---

### 2. Trace 捕获工具

**文件：** `test/src/main/java/org/autorewriter/e2e/wetune/WeTuneTraceCaptureRunner.java`

这是一次性运行的工具类（不是 JUnit 测试），作为 `main()` 方法执行。

**流程：**
1. 加载 `wetune_rules.txt`（650 条规则），解析为 `RuleAnalysisContext` 列表
2. 对每个 app，读取所有 `query/q{stmtId}.sql`
3. 使用 AutoRewriter 的 RBO（MANUAL pipeline，`HepPlanner`）优化每条查询
4. 从 `OptimizationTrace` 中提取每个优化结果的规则序列：
   - 通过 `pathSummary()` 获取所有独立优化路径
   - 每条路径记录触发的 `AutoRewriteRule` 对应的规则行号
5. 序列化为 JSON 写入 `test/resources/schema/wetune_{app}/traces/{stmtId}.json`

**规则行号获取：**
- `AutoRewriteRule` 持有对应 `RuleAnalysisContext`，上下文中记录规则在文件中的行号（需在 `readRuleAnalysisContexts()` 时保存行号）
- 或者通过规则模板字符串在 `rules.txt` 中查找行号

**JSON 格式：**
```json
{
  "appName": "wetune_diaspora",
  "stmtId": 202,
  "inputSql": "SELECT COUNT(...) FROM ...",
  "paths": [
    {
      "pathIndex": 0,
      "ruleLines": [42, 7],
      "optimizedSql": "SELECT COUNT(...) FROM ..."
    },
    {
      "pathIndex": 1,
      "ruleLines": [15],
      "optimizedSql": "SELECT COUNT(...) FROM ..."
    }
  ]
}
```

---

### 3. 回归验证测试

**文件：** `test/src/main/java/org/autorewriter/e2e/wetune/AutoRwWeTuneTraceVerifyTest.java`

继承 `AutoRwFakeE2ETesBase`，JUnit 5 测试类。

**每个 app 一个 `@Test` 方法，例如：**
```java
@Test
void testWeTuneDiaspora() {
    verifyApp("wetune_diaspora");
}
```

**`verifyApp()` 流程：**
1. 加载该 app 所有 `traces/{stmtId}.json`（expected）
2. 使用 `wetune_rules.txt` 作为规则集，执行 RBO pipeline（MANUAL 模式）
3. 对每条查询：
   - 对比实际触发的规则序列 vs expected 中每个 path 的 `ruleLines`
   - 匹配逻辑：**实际路径集合**与**expected 路径集合**一一对应（无需全部匹配，只要 expected 中每个 path 都能在实际中找到对应的规则序列即可）
   - 结果分类：`EXACT`（完全匹配）/ `EXTRA`（实际多触发了规则）/ `MISSING`（expected 中有但实际未触发）/ `ERROR`（解析/优化失败）
4. 生成汇总报告（类似 `RewritePathE2ETest` 的表格）

**失败条件：** 任何 `MISSING` 或 `ERROR` 视为测试失败。

---

### 4. RuleAnalysisContext 行号支持

**修改文件：** `rewriter/src/main/java/org/autorewriter/rewriter/analyze/RuleAnalysisContext.java`  
**修改文件：** `rewriter/src/main/java/org/autorewriter/rewriter/analyze/RuleAnalyzer.java`

在 `RuleAnalysisContext` 中新增 `int ruleLineNumber` 字段，在 `readRuleAnalysisContexts()` 解析时记录行号（1-based，与 WeTune 一致）。

**修改文件：** `rewriter/src/main/java/org/autorewriter/rewriter/rule/AutoRewriteRule.java`

新增 `getRuleLineNumber()` 方法，将行号暴露给 trace 捕获器。

---

### 5. 规则文件

**操作：** 将 `wetune/wtune_data/prepared/rules.txt` 复制到  
`test/src/main/resources/wetune_rules.txt`

不做任何修改，保持与 WeTune 完全一致的规则集（650 条）。

---

## 不在范围内

- CBO（VolcanoPlanner）验证（WeTune 没有 CBO）
- WeTune 规则语义正确性验证（不验证优化后查询是否等价）
- 性能测试
- AutoRewriter 自有规则集（`6t2n.txt` 等）的迁移（独立任务）

---

## 文件清单

| 文件 | 类型 | 说明 |
|---|---|---|
| `scripts/migrate_wetune_data.py` | 新建 | 数据迁移脚本 |
| `test/resources/wetune_rules.txt` | 新建 | WeTune 规则文件副本 |
| `test/resources/schema/wetune_{app}/table_ddl.sql` | 新建×21 | 各 app schema |
| `test/resources/schema/wetune_{app}/query/q{id}.sql` | 新建×N | 各 app 查询 |
| `test/resources/schema/wetune_{app}/traces/{id}.json` | 新建×N | 捕获的规则 trace |
| `WeTuneTraceCaptureRunner.java` | 新建 | 一次性 trace 捕获工具 |
| `AutoRwWeTuneTraceVerifyTest.java` | 新建 | 回归验证测试 |
| `RuleAnalysisContext.java` | 修改 | 新增 `ruleLineNumber` 字段 |
| `RuleAnalyzer.java` | 修改 | 解析时记录行号 |
| `AutoRewriteRule.java` | 修改 | 暴露 `getRuleLineNumber()` |
