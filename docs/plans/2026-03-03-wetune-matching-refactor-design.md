# WeTune-Style Matching & Rewriting Refactor Design

## Goal

Refactor AutoRewriter's rule matching and rewriting engine to replicate WeTune's algorithm on top of Calcite RelNode, achieving equivalent matching power including:
- Symbol-based strong-typed bindings with Union-Find equivalence classes
- Model with derive()/rollback for backtracking
- Join tree permutation matching
- Filter chain combinatorial matching
- Constraint-driven instantiation for target construction

## Scope

**In scope:**
- Symbol + Model binding system (replaces `Map<String, Object>`)
- Constraints with Union-Find equivalence classes and instantiation mapping
- Recursive matching engine with Join permutation and Filter combinatorial matching
- Instantiation-based target RelNode construction
- InSubFilter special handling (extract `RexSubQuery.in` from `LogicalFilter.condition`)
- Operators: INPUT, PROJ, PROJ*, FILTER, INNER_JOIN, LEFT_JOIN, IN_SUB_FILTER

**Out of scope:**
- Rule parsing (ShardingSphere ANTLR4 grammar preserved)
- Optimizer framework (Calcite HepPlanner/RuleBaseOptimizer preserved)
- Rule enumeration / SMT verification
- New operator types (EXISTS, SORT, LIMIT, UNION)

## Architecture

### 1. Symbol + Model

`Symbol(Kind, name)` — strong-typed placeholder extracted from template RelNode:
- `TABLE "t0"` → binds to `RelNode` (any query subtree)
- `ATTRS "a0"` → binds to `List<ColumnRef>` (concrete column references)
- `PRED  "p0"` → binds to `RexNode` (predicate expression)
- `SCHEMA "s0"` → binds to `RelDataType` (output schema)

`Model` — Symbol → Value mapping with:
- `assign(symbol, value)` — assigns value, checks Union-Find equivalence class consistency, returns false on conflict
- `checkConstraints()` — validates integrity constraints (Unique, NotNull, Reference, AttrsSub) against current assignments
- `derive()` → child Model (copy-on-write snapshot); discard child to rollback on match failure
- `getTable(sym)`, `getAttrs(sym)`, `getPred(sym)`, `getSchema(sym)` — typed accessors

`NaturalCongruence<Symbol>` — Union-Find tracking equivalence classes from TableEq/AttrsEq/PredicateEq/SchemaEq constraints.

### 2. Constraints

Constructed once from `ConstraintSegment` list + template RelNodes:
- `eqClasses: NaturalCongruence<Symbol>` — Union-Find for equality constraints
- `instantiation: Map<Symbol, Symbol>` — target symbol → source symbol mapping (cross-template equalities)
- `integrityConstraints: List<Constraint>` — Unique, NotNull, Reference, AttrsSub
- `ofKind(kind)` — segment-based fast access by constraint kind
- `instantiationOf(targetSym)` → source Symbol
- `sourceOf(attrsSym)` → AttrsSub source table Symbol

### 3. Match (Recursive Matching Engine)

Entry: `Match.match(sourceTemplate, queryRelNode, model)`

Type-dispatched recursive matching:

- **INPUT** (`LogicalTableScan` template with `t\d+` name): wildcard-matches any RelNode subtree. `model.assign(tableSymbol, queryNode)` + `model.checkConstraints()`.
- **PROJ** (`LogicalProject`): match projection — assign attrs/schema symbols from query fields, recurse on child.
- **FILTER** (`LogicalFilter`): `FilterMatcher` collects consecutive filter chain from query, enumerates combinatorial assignments of template filters to chain filters, each with `model.derive()` for backtracking.
- **JOIN** (`LogicalJoin`): `JoinMatcher` builds `LinearJoinTree` (flattens query join tree), tries matching template join at each position with LHS/RHS flip, each with `model.derive()`.
- **AGG** (`LogicalAggregate`): matches DISTINCT (Proj*) — assign group-by attrs, recurse on child.
- **IN_SUB_FILTER**: detects `RexSubQuery.in` in `LogicalFilter.condition`, extracts subquery as virtual second child, decomposes `AND(in_sub, other)` if needed, assigns correlation attrs, recurses on both main input and subquery.

### 4. Instantiation (Target Construction)

Entry: `Instantiation.instantiate(targetTemplate, model, constraints)`

Walks target template RelNode top-down:

- **INPUT**: `constraints.instantiationOf(t_target)` → `t_source` → `model.getTable(t_source)` → return bound RelNode
- **PROJ**: instantiate child, resolve `a_target` → `a_source` → `model.getAttrs(a_source)` for projection, resolve `s_target` → `s_source` → `model.getSchema(s_source)` for field names, `LogicalProject.create()`
- **FILTER**: instantiate child, resolve `p_target` → `p_source` → `model.getPred(p_source)`, create `LogicalFilter`
- **JOIN**: instantiate left/right, rebuild join condition from bound column refs
- **AGG**: instantiate child, rebuild group-by from bound attrs
- **IN_SUB_FILTER**: instantiate main + subquery child, rebuild `RexSubQuery.in`

RexNode column references (`RexInputRef`) are rebound using `ColumnRef` for stable identity + `ColumnRefResolver` for index lookup in new child.

### 5. AutoRewriteRule (Rewritten)

```java
public class AutoRewriteRule extends RelOptRule {
    private final Constraints constraints;
    private final RelNode sourceTemplate;
    private final RelNode targetTemplate;
    private Model lastMatchModel;

    // Constructor: parse ConstraintSegments + extract Symbols → Constraints

    boolean matches(call):
        model = new Model(constraints)
        success = Match.match(sourceTemplate, call.rel(0), model)
        if success: lastMatchModel = model
        return success

    void onMatch(call):
        result = Instantiation.instantiate(targetTemplate, lastMatchModel, constraints)
        result = adjustRowType(result, call.rel(0).getRowType())
        call.transformTo(result)
}
```

### 6. File Structure

New files under `rewriter/src/main/java/org/autorewriter/rewriter/rule/`:

```
rule/
├── AutoRewriteRule.java              ← rewrite (keep name, use new internals)
├── symbol/
│   ├── Symbol.java
│   ├── SymbolKind.java
│   └── SymbolExtractor.java
├── model/
│   ├── Model.java
│   └── NaturalCongruence.java
├── constraint/
│   ├── Constraint.java
│   ├── ConstraintKind.java
│   └── Constraints.java
├── match/
│   ├── Match.java
│   ├── JoinMatcher.java
│   ├── FilterMatcher.java
│   └── LinearJoinTree.java
├── instantiation/
│   └── Instantiation.java
└── util/
    ├── ColumnRef.java                ← keep
    └── ColumnRefResolver.java        ← keep
```

Deleted files:
- `RelNodeMatcher.java`, `RelNodeFiller.java`
- `matcher/*Matcher.java` (5 files)
- `filler/*Filler.java` (5 files)
- `constraint/ConstraintEvaluator.java`, `ConstraintHandler.java`, `ConstraintUtils.java`, `BindingResolver.java`
- `constraint/handler/*Handler.java` (7 files)
- `util/RexNodeMatcher.java`, `util/RexNodeFiller.java`

### 7. Unchanged Components

- `RuleAnalyzer.java` — still delegates to ShardingSphere parser
- `RuleAnalysisContext.java` — still wraps source/target RelNode + constraint segments
- `RuleBaseOptimizer.java` — still wraps HepPlanner
- `ManualProducePipeline.java` — still orchestrates end-to-end flow
- `RuleTemplateSimplifier.java` — still pre-processes template RelNodes
- All test infrastructure
