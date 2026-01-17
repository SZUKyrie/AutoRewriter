package org.autorewriter.sql.analyze;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.config.CalciteSystemProperty;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.plan.*;
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.prepare.CalciteSqlValidator;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.metadata.CachingRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexExecutor;
import org.apache.calcite.runtime.Hook;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.util.SqlOperatorTables;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql2rel.RelDecorrelator;
import org.apache.calcite.sql2rel.SqlRexConvertletTable;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.calcite.tools.*;
import org.apache.calcite.util.Pair;
import org.apache.calcite.util.SourceStringReader;
import org.autorewriter.common.enums.ComputeEngine;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Reader;
import java.util.List;
import java.util.Objects;

import org.apache.shardingsphere.sqlfederation.autorewriter.MultiDialectSqlParser;

/**
 * Planner for multi dialect, copy from {@link org.apache.calcite.prepare.PlannerImpl} except parser
 */
public class MultiDialectPlanner implements Planner, RelOptTable.ViewExpander {

    /**
     * Extra field for dialect, decide which parser to use
     */
    private ComputeEngine computeEngine;

    private final SqlOperatorTable operatorTable;
    private final ImmutableList<Program> programs;
    private final @Nullable RelOptCostFactory costFactory;
    private final Context context;
    private final CalciteConnectionConfig connectionConfig;
    private final RelDataTypeSystem typeSystem;
    private final @Nullable ImmutableList<RelTraitDef> traitDefs;
    private final SqlParser.Config parserConfig;
    private final SqlValidator.Config sqlValidatorConfig;
    private final SqlToRelConverter.Config sqlToRelConverterConfig;
    private final SqlRexConvertletTable convertletTable;
    private State state;
    private boolean open;
    private @Nullable SchemaPlus defaultSchema;
    private @Nullable JavaTypeFactory typeFactory;
    private @Nullable RelOptPlanner planner;
    private @Nullable RexExecutor executor;
    private @Nullable SqlValidator validator;
    private @Nullable SqlNode validatedSqlNode;

    public MultiDialectPlanner(FrameworkConfig config, ComputeEngine computeEngine) {
        this.computeEngine = computeEngine;
        this.costFactory = config.getCostFactory();
        this.defaultSchema = config.getDefaultSchema();
        this.operatorTable = config.getOperatorTable();
        this.programs = config.getPrograms();
        this.parserConfig = config.getParserConfig();
        this.sqlValidatorConfig = config.getSqlValidatorConfig();
        this.sqlToRelConverterConfig = config.getSqlToRelConverterConfig();
        this.state = State.STATE_0_CLOSED;
        this.traitDefs = config.getTraitDefs();
        this.convertletTable = config.getConvertletTable();
        this.executor = config.getExecutor();
        this.context = config.getContext();
        this.connectionConfig = connConfig(this.context, this.parserConfig);
        this.typeSystem = config.getTypeSystem();
        this.reset();
    }

    private static CalciteConnectionConfig connConfig(Context context, SqlParser.Config parserConfig) {
        CalciteConnectionConfigImpl config = (CalciteConnectionConfigImpl) context.maybeUnwrap(CalciteConnectionConfigImpl.class)
                .orElse(CalciteConnectionConfig.DEFAULT);
        if (!config.isSet(CalciteConnectionProperty.CASE_SENSITIVE)) {
            config = config.set(CalciteConnectionProperty.CASE_SENSITIVE, String.valueOf(parserConfig.caseSensitive()));
        }

        if (!config.isSet(CalciteConnectionProperty.CONFORMANCE)) {
            config = config.set(CalciteConnectionProperty.CONFORMANCE, String.valueOf(parserConfig.conformance()));
        }

        return config;
    }

    private void ensure(State state) {
        if (state != this.state) {
            if (state.ordinal() < this.state.ordinal()) {
                throw new IllegalArgumentException("cannot move to " + state + " from " + this.state);
            } else {
                state.from(this);
            }
        }
    }

    @Override
    public RelTraitSet getEmptyTraitSet() {
        return ((RelOptPlanner) Objects.requireNonNull(this.planner, "planner")).emptyTraitSet();
    }

    @Override
    public void close() {
        this.open = false;
        this.typeFactory = null;
        this.state = State.STATE_0_CLOSED;
    }

    @Override
    public void reset() {
        this.ensure(State.STATE_0_CLOSED);
        this.open = true;
        this.state = State.STATE_1_RESET;
    }

    @SuppressWarnings({"checkstyle:MissingSwitchDefault", "checkstyle:InnerAssignment"})
    private void ready() {
        switch (this.state) {
            case STATE_0_CLOSED:
                this.reset();
        }

        this.ensure(State.STATE_1_RESET);
        this.typeFactory = new JavaTypeFactoryImpl(this.typeSystem);
        RelOptPlanner planner = this.planner = new VolcanoPlanner(this.costFactory, this.context);
        RelOptUtil.registerDefaultRules(planner, this.connectionConfig.materializationsEnabled(), (Boolean) Hook.ENABLE_BINDABLE.get(false));
        planner.setExecutor(this.executor);
        this.state = State.STATE_2_READY;
        if (this.traitDefs == null) {
            planner.addRelTraitDef(ConventionTraitDef.INSTANCE);
            if ((Boolean) CalciteSystemProperty.ENABLE_COLLATION_TRAIT.value()) {
                planner.addRelTraitDef(RelCollationTraitDef.INSTANCE);
            }
        } else {
            UnmodifiableIterator var2 = this.traitDefs.iterator();

            while (var2.hasNext()) {
                RelTraitDef def = (RelTraitDef) var2.next();
                planner.addRelTraitDef(def);
            }
        }
    }

    /**
     * Skip parse stage, change state to parsed, allow use other parser to parse sql
     */
    public void skipParse() {
        switch (this.state) {
            case STATE_0_CLOSED:
            case STATE_1_RESET:
                this.ready();
            default:
                this.ensure(State.STATE_2_READY);
                this.state = State.STATE_3_PARSED;
        }
    }

    /**
     * Choose the corresponding parser according to the dialect.
     * ClickHouse sql use MultiDialectSqlParser of Shardingsphere, other sql use Calcite parser
     */
    @Override
    public SqlNode parse(Reader reader) throws SqlParseException {
        switch (this.state) {
            case STATE_0_CLOSED:
            case STATE_1_RESET:
                this.ready();
            default:
                this.ensure(State.STATE_2_READY);
                SqlNode sqlNode = computeEngine.useMultiDialectParser()
                        ? parseByMultiDialectParser(reader)
                        : parseByCalciteParser(reader);
                this.state = State.STATE_3_PARSED;
                return sqlNode;
        }
    }

    private SqlNode parseByCalciteParser(Reader reader) throws SqlParseException {
        SqlParser parser = SqlParser.create(reader, this.parserConfig);
        return parser.parseStmt();
    }

    private SqlNode parseByMultiDialectParser(Reader reader) {
        if (!(reader instanceof SourceStringReader)) {
            throw new IllegalArgumentException("reader must be SourceStringReader");
        }
        String sql = ((SourceStringReader) reader).getSourceString();
        MultiDialectSqlParser parser = MultiDialectSqlParserFactory.createParser(this.computeEngine);
        return parser.parse(sql);
    }

    @Override
    @EnsuresNonNull({"validator"})
    public SqlNode validate(SqlNode sqlNode) throws ValidationException {
        //this.ensure(State.STATE_3_PARSED);
        this.validator = this.createSqlValidator(this.createCatalogReader());

        try {
            this.validatedSqlNode = this.validator.validate(sqlNode);
        } catch (RuntimeException var3) {
            throw new ValidationException(var3);
        }

        this.state = State.STATE_4_VALIDATED;
        return this.validatedSqlNode;
    }

    @Override
    public Pair<SqlNode, RelDataType> validateAndGetType(SqlNode sqlNode) throws ValidationException {
        SqlNode validatedNode = this.validate(sqlNode);
        RelDataType type = this.validator.getValidatedNodeType(validatedNode);
        return Pair.of(validatedNode, type);
    }

    @Override
    public RelDataType getParameterRowType() {
        if (this.state.ordinal() < State.STATE_4_VALIDATED.ordinal()) {
            throw new RuntimeException("Need to call #validate() first");
        } else {
            return ((SqlValidator) Objects.requireNonNull(this.validator, "validator"))
                    .getParameterRowType((SqlNode) Objects.requireNonNull(this.validatedSqlNode, "validatedSqlNode"));
        }
    }

    @Override
    public final RelNode convert(SqlNode sql) {
        return this.rel(sql).rel;
    }

    @Override
    public RelRoot rel(SqlNode sql) {
        this.ensure(State.STATE_4_VALIDATED);
        SqlNode validatedSqlNode = (SqlNode) Objects.requireNonNull(this.validatedSqlNode,
                "validatedSqlNode is null. Need to call #validate() first");
        RexBuilder rexBuilder = this.createRexBuilder();
        RelOptCluster cluster = RelOptCluster.create((RelOptPlanner) Objects.requireNonNull(this.planner, "planner"), rexBuilder);
        SqlToRelConverter.Config config = this.sqlToRelConverterConfig.withTrimUnusedFields(false);
        SqlToRelConverter sqlToRelConverter = new SqlToRelConverter(this, this.validator,
                this.createCatalogReader(), cluster, this.convertletTable, config);
        RelRoot root = sqlToRelConverter.convertQuery(validatedSqlNode, false, true);
        root = root.withRel(sqlToRelConverter.flattenTypes(root.rel, true));
        RelBuilder relBuilder = config.getRelBuilderFactory().create(cluster, (RelOptSchema) null);
        root = root.withRel(RelDecorrelator.decorrelateQuery(root.rel, relBuilder));
        this.state = State.STATE_5_CONVERTED;
        return root;
    }

    @Override
    public RelRoot expandView(RelDataType rowType, String queryString, List<String> schemaPath, @Nullable List<String> viewPath) {
        RelOptPlanner planner = this.planner;
        if (planner == null) {
            this.ready();
            planner = (RelOptPlanner) Objects.requireNonNull(this.planner, "planner");
        }

        SqlParser parser = SqlParser.create(queryString, this.parserConfig);

        SqlNode sqlNode;
        try {
            sqlNode = parser.parseQuery();
        } catch (SqlParseException var17) {
            throw new RuntimeException("parse failed", var17);
        }

        CalciteCatalogReader catalogReader = this.createCatalogReader().withSchemaPath(schemaPath);
        SqlValidator validator = this.createSqlValidator(catalogReader);
        RexBuilder rexBuilder = this.createRexBuilder();
        RelOptCluster cluster = RelOptCluster.create(planner, rexBuilder);
        SqlToRelConverter.Config config = this.sqlToRelConverterConfig.withTrimUnusedFields(false);
        SqlToRelConverter sqlToRelConverter = new SqlToRelConverter(this, validator, catalogReader, cluster, this.convertletTable, config);
        RelRoot root = sqlToRelConverter.convertQuery(sqlNode, true, false);
        RelRoot root2 = root.withRel(sqlToRelConverter.flattenTypes(root.rel, true));
        RelBuilder relBuilder = config.getRelBuilderFactory().create(cluster, (RelOptSchema) null);
        return root2.withRel(RelDecorrelator.decorrelateQuery(root.rel, relBuilder));
    }

    private CalciteCatalogReader createCatalogReader() {
        SchemaPlus defaultSchema = (SchemaPlus) Objects.requireNonNull(this.defaultSchema, "defaultSchema");
        SchemaPlus rootSchema = rootSchema(defaultSchema);
        return new CalciteCatalogReader(CalciteSchema.from(rootSchema),
                CalciteSchema.from(defaultSchema).path((String) null), this.getTypeFactory(), this.connectionConfig);
    }

    private SqlValidator createSqlValidator(CalciteCatalogReader catalogReader) {
        final SqlOperatorTable opTab =
                SqlOperatorTables.chain(operatorTable, catalogReader);
        return new CalciteSqlValidator(opTab,
                catalogReader,
                getTypeFactory(),
                sqlValidatorConfig
                        .withLenientOperatorLookup(connectionConfig.lenientOperatorLookup())
                        .withConformance(connectionConfig.conformance())
                        .withIdentifierExpansion(true));
    }

    private static SchemaPlus rootSchema(SchemaPlus schema) {
        while (true) {
            SchemaPlus parentSchema = schema.getParentSchema();
            if (parentSchema == null) {
                return schema;
            }

            schema = parentSchema;
        }
    }

    private RexBuilder createRexBuilder() {
        return new RexBuilder(this.getTypeFactory());
    }

    @Override
    public JavaTypeFactory getTypeFactory() {
        return (JavaTypeFactory) Objects.requireNonNull(this.typeFactory, "typeFactory");
    }

    @Override
    public RelNode transform(int ruleSetIndex, RelTraitSet requiredOutputTraits, RelNode rel) {
        this.ensure(State.STATE_5_CONVERTED);
        rel.getCluster().setMetadataProvider(new CachingRelMetadataProvider((RelMetadataProvider)
                Objects.requireNonNull(rel.getCluster().getMetadataProvider(), "metadataProvider"), rel.getCluster().getPlanner()));
        Program program = (Program) this.programs.get(ruleSetIndex);
        return program.run((RelOptPlanner) Objects.requireNonNull(this.planner, "planner"),
                rel, requiredOutputTraits, ImmutableList.of(), ImmutableList.of());
    }

    private static enum State {
        STATE_0_CLOSED {
            @Override
            void from(MultiDialectPlanner planner) {
                planner.close();
            }
        },
        STATE_1_RESET {
            @Override
            void from(MultiDialectPlanner planner) {
                planner.ensure(STATE_0_CLOSED);
                planner.reset();
            }
        },
        STATE_2_READY {
            @Override
            void from(MultiDialectPlanner planner) {
                STATE_1_RESET.from(planner);
                planner.ready();
            }
        },
        STATE_3_PARSED,
        STATE_4_VALIDATED,
        STATE_5_CONVERTED;

        private State() {
        }

        void from(MultiDialectPlanner planner) {
            throw new IllegalArgumentException("cannot move from " + planner.state + " to " + this);
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public class ViewExpanderImpl implements RelOptTable.ViewExpander {
        ViewExpanderImpl() {
        }

        @Override
        public RelRoot expandView(RelDataType rowType, String queryString, List<String> schemaPath, @Nullable List<String> viewPath) {
            return MultiDialectPlanner.this.expandView(rowType, queryString, schemaPath, viewPath);
        }
    }
}
