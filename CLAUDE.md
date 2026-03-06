# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AutoRewriter is a template-based SQL query rewriting engine that automatically rewrites SQL queries using large-scale rewrite rules. It converts SQL queries to relational algebra (Apache Calcite `RelNode` trees), pattern-matches them against rule templates, and rewrites them into optimized equivalents (e.g., removing unnecessary DISTINCT, converting LEFT JOIN to INNER JOIN when constraints allow).

## Build & Test Commands

**Prerequisites:** Java 11+ and Maven 3.x. First-time setup requires installing custom JARs:

```bash
./install-local-jars.sh    # Install custom Calcite + ShardingSphere JARs to local Maven repo
```

```bash
mvn clean install                    # Full build
mvn clean install -DskipTests        # Build without tests
mvn test                             # Run all tests
mvn test -pl rewriter                # Run tests in a specific module
mvn test -pl rewriter -Dtest=RuleBaseOptimizerTest                                    # Single test class
mvn test -pl rewriter -Dtest=RuleBaseOptimizerTest#testRemoveDistinctWithUniqueConstraint  # Single test method
```

The `test` module contains E2E tests that require a local PostgreSQL instance (`localhost:5432`, user `postgres`/`postgres`). Unit tests in `rewriter` and other modules do not require external databases.

No linter is configured.

## Architecture

### Module Dependency Graph

```
test (E2E/integration tests)
  └── rewriter (core rewrite engine)
        ├── sql-core (SQL parsing, dialect support via ShardingSphere, Calcite planner setup)
        ├── common (type converters, data transformers)
        ├── meta/external-meta (schema metadata: PostgresJDBC, Calcite schema registry)
        └── model (enums, entities, constants, exceptions — Java 17)
```

### Data Flow

1. **Rule parsing:** Rule template strings → `RuleAnalyzer` → `RuleAnalysisContext` (source RelNode template, target RelNode template, match/rewrite constraints)
2. **SQL parsing:** SQL string → `SqlAnalyzer` (ShardingSphere multi-dialect parser) → `SqlNode` → validated → `RelNode`
3. **Optimization:** `RuleBaseOptimizer` (wraps Calcite `HepPlanner`) applies `AutoRewriteRule` instances which recursively match query `RelNode` trees against source templates, check constraints via `ConstraintEvaluator`, and fill target templates
4. **Output:** Optimized `RelNode` → SQL via `RelToSqlConverter`

### Key Packages in `rewriter`

- `analyze/` — `RuleAnalyzer` parses rule template strings into `RuleAnalysisContext`
- `rule/` — `AutoRewriteRule` (extends Calcite `RelOptRule`), `RelNodeMatcher`, `RelNodeFiller`
- `rule/matcher/` — Per-node-type matchers: `ProjectMatcher`, `FilterMatcher`, `JoinMatcher`, `AggregateMatcher`, `TableScanMatcher`
- `rule/filler/` — Per-node-type fillers that construct target `RelNode` trees from bindings
- `rule/constraint/` — `ConstraintEvaluator` and handlers: `UniqueConstraintHandler`, `NotNullConstraintHandler`, `ReferenceConstraintHandler`, `AttrsEqConstraintHandler`, `TableEqConstraintHandler`, `PredicateEqConstraintHandler`, `AttrsSubConstraintHandler`
- `optimize/ruleBaseOpt/` — `RuleBaseOptimizer`, `RuleTemplateSimplifier`
- `pipleline/` — `ProducePipeline`, `ManualProducePipeline`, `ProduceStage`, `ProduceContext` (note: "pipleline" is an intentional directory name typo in the codebase)

### Rule Template Format

Rules are pipe-delimited strings with 3 parts:
```
<source_template>|<target_template>|<match_constraints>;<rewrite_constraints>
```

- **Node types:** `Proj` (project), `Proj*` (distinct/aggregate), `Filter`, `InnerJoin`, `LeftJoin`, `InSubFilter`, `Input` (table scan)
- **Placeholders:** `<t0>` (table), `<a0>` (attributes), `<p0>` (predicates), `<s0>` (schema)
- **Constraints:** `Unique(t,a)`, `NotNull(t,a)`, `Reference(t1,a1,t2,a2)`, `TableEq(t1,t2)`, `AttrsEq(a1,a2)`, `AttrsSub(a,t)`, `PredicateEq(p1,p2)`, `SchemaEq(s1,s2)`

### Custom Forks

This project uses custom forks of Apache Calcite (`1.38.0-U6` / `1.37.0-U8` for linq4j) and ShardingSphere SQL Federation (`5.5.4-SNAPSHOT`), shipped as JARs in `libs/` and installed via `install-local-jars.sh`.

## Tech Stack

- **Language:** Java 11 (Java 17 for `model` and `test` modules)
- **Build:** Maven 3.x
- **SQL parsing:** Apache ShardingSphere SQL Federation (custom fork)
- **Relational algebra:** Apache Calcite (custom fork), HepPlanner for rule-based optimization
- **Code generation:** Lombok (annotations), Janino (Calcite runtime)
- **Testing:** JUnit 5, Mockito
- **Logging:** SLF4J 2.x + Logback