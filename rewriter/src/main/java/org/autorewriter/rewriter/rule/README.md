# AutoRewriteRule 架构说明

## 概述

`AutoRewriteRule` 已经重构为基于组件的架构，将不同算子的处理逻辑拆分到独立的文件中，便于维护和扩展。

## 目录结构

```
rule/
├── AutoRewriteRule.java          # 主规则类（核心调度逻辑）
├── RelNodeMatcher.java            # 匹配器接口
├── RelNodeFiller.java             # 填充器接口
├── matcher/                       # 匹配器实现
│   ├── TableScanMatcher.java     # TableScan 匹配器
│   ├── ProjectMatcher.java       # Project 匹配器
│   ├── FilterMatcher.java        # Filter 匹配器
│   ├── JoinMatcher.java          # Join 匹配器
│   └── AggregateMatcher.java     # Aggregate 匹配器
├── filler/                        # 填充器实现
│   ├── TableScanFiller.java      # TableScan 填充器
│   ├── ProjectFiller.java        # Project 填充器
│   ├── FilterFiller.java         # Filter 填充器
│   ├── JoinFiller.java           # Join 填充器
│   └── AggregateFiller.java      # Aggregate 填充器
├── constraint/                    # 约束评估
│   └── ConstraintEvaluator.java  # 约束评估器
└── util/                          # 工具类
    ├── RexNodeMatcher.java       # RexNode 匹配工具
    └── RexNodeFiller.java        # RexNode 填充工具
```

## 组件说明

### 1. 核心接口

- **RelNodeMatcher<T>**: 定义算子匹配逻辑的接口
  - `match(T template, T query, Map<String, Object> bindings)`: 匹配模板与查询节点

- **RelNodeFiller<T>**: 定义算子填充逻辑的接口
  - `fill(T template, Map<String, Object> bindings)`: 用绑定值填充模板

### 2. 匹配器（Matcher）

每个算子都有对应的匹配器，负责：
- 验证模板与查询节点是否匹配
- 将占位符绑定到实际值
- 递归匹配子节点

示例：
```java
public class FilterMatcher implements RelNodeMatcher<LogicalFilter> {
    @Override
    public boolean match(LogicalFilter template, LogicalFilter query, Map<String, Object> bindings) {
        // 匹配逻辑
    }
}
```

### 3. 填充器（Filler）

每个算子都有对应的填充器，负责：
- 使用绑定值填充目标模板
- 创建新的 RelNode 实例
- 处理类型和字段映射

示例：
```java
public class FilterFiller implements RelNodeFiller<LogicalFilter> {
    @Override
    public RelNode fill(LogicalFilter template, Map<String, Object> bindings) {
        // 填充逻辑
    }
}
```

### 4. 约束评估器（ConstraintEvaluator）

负责评估匹配和改写约束：
- `checkMatchConstraints()`: 检查匹配约束
- `applyRewriteConstraints()`: 应用改写约束
- 支持的约束类型：PREDICATE_EQ, ATTRS_EQ, TABLE_EQ, ATTRS_SUB 等

### 5. 工具类（Util）

- **RexNodeMatcher**: 处理 RexNode 表达式的匹配
- **RexNodeFiller**: 处理 RexNode 表达式的填充

## 如何添加新算子

添加新算子只需三步：

### 步骤 1: 创建匹配器

在 `matcher/` 目录下创建新文件：

```java
public class NewOperatorMatcher implements RelNodeMatcher<LogicalNewOperator> {
    private final BiFunction<RelNode, RelNode, Boolean> recursiveMatchFunc;
    
    @Override
    public boolean match(LogicalNewOperator template, LogicalNewOperator query, 
                        Map<String, Object> bindings) {
        // 实现匹配逻辑
        if (!recursiveMatchFunc.apply(template.getInput(), query.getInput())) {
            return false;
        }
        // ... 其他匹配逻辑
        return true;
    }
}
```

### 步骤 2: 创建填充器

在 `filler/` 目录下创建新文件：

```java
public class NewOperatorFiller implements RelNodeFiller<LogicalNewOperator> {
    private final Function<RelNode, RelNode> fillTargetTemplateFunc;
    
    @Override
    public RelNode fill(LogicalNewOperator template, Map<String, Object> bindings) {
        RelNode filledInput = fillTargetTemplateFunc.apply(template.getInput());
        // 创建并返回新的 RelNode
        return LogicalNewOperator.create(filledInput, ...);
    }
}
```

### 步骤 3: 在 AutoRewriteRule 中注册

在 `AutoRewriteRule.java` 的构造函数中添加：

```java
private final NewOperatorMatcher newOperatorMatcher;
private final NewOperatorFiller newOperatorFiller;

public AutoRewriteRule(RelOptRuleOperand operand, RuleAnalysisContext ruleContext) {
    // ...existing code...
    this.newOperatorMatcher = new NewOperatorMatcher(this::recursiveMatchInternal);
    this.newOperatorFiller = new NewOperatorFiller(this::fillTargetTemplate);
}
```

在 `recursiveMatch()` 方法中添加分支：

```java
private boolean recursiveMatch(RelNode template, RelNode query, Map<String, Object> bindings) {
    // ...existing code...
    } else if (template instanceof LogicalNewOperator) {
        return newOperatorMatcher.match((LogicalNewOperator) template, (LogicalNewOperator) query, bindings);
    }
    // ...existing code...
}
```

在 `fillTargetTemplate()` 方法中添加分支：

```java
private RelNode fillTargetTemplate(RelNode template, Map<String, Object> bindings) {
    // ...existing code...
    } else if (template instanceof LogicalNewOperator) {
        return newOperatorFiller.fill((LogicalNewOperator) template, bindings);
    }
    // ...existing code...
}
```

## 优势

1. **代码清晰**: 每个算子的逻辑独立，易于理解
2. **易于维护**: 修改某个算子不影响其他算子
3. **便于扩展**: 添加新算子只需要添加新文件，不需要修改大量现有代码
4. **职责分离**: 匹配、填充、约束评估各司其职
5. **可测试性**: 每个组件都可以独立测试

## 总结

重构后的 `AutoRewriteRule` 从原来的 700+ 行单一文件，拆分为 15 个小文件，每个文件专注于特定功能。这种模块化设计使代码更加清晰、易于维护，并为未来添加新算子提供了良好的扩展性。
