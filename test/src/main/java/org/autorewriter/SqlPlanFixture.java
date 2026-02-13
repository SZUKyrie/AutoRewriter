package org.autorewriter;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.util.SqlString;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.autorewriter.common.enums.ComputeEngine;
import org.autorewriter.common.enums.TableEngine;
import org.autorewriter.meta.schema.CalciteSchemaRegistry;
import org.autorewriter.meta.schema.KwaiCalciteSchema;
import org.autorewriter.meta.schema.ProxySchema;
import org.autorewriter.meta.schema.postgres.PostgresJdbcSchemaService;
import org.autorewriter.sql.analyze.AnalysisContext;
import org.autorewriter.sql.analyze.SqlAnalyzer;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.autorewriter.common.constant.NotationConstants.EMPTY_STRING;
import static org.autorewriter.sql.analyze.AnalysisConfigs.COMMON_CALCITE_CONNECTION_CONFIG;
import static org.junit.Assert.assertEquals;

/**
 * SQL Plan Fixture， For Testing
 */
@Slf4j
public class SqlPlanFixture {

    private static final String dbStorePath = "./target/postgres-test/";
    private static final String driverClassName = "org.postgresql.Driver";

    private ComputeEngine computeEngine = ComputeEngine.POSTGRESQL;
    protected String db = "test";
    protected List<String> createTableSQLs = new ArrayList<>();
    private String sql;
    protected CalciteCatalogReader catalogReader;
    private RelDataTypeSystem typeSystem = RelDataTypeSystem.DEFAULT;
    protected RelDataTypeFactory typeFactory = new SqlTypeFactoryImpl(typeSystem);
    private RelNode plan;
    private Planner planner;

    public SqlPlanFixture() {
        catalogReader = new CalciteCatalogReader(CalciteSchema.from(getPostgresDataSourceSchema()),
                new ArrayList<>(), typeFactory, COMMON_CALCITE_CONNECTION_CONFIG);
    }

    public SqlPlanFixture(SqlPlanFixture other) {
        this.computeEngine = other.computeEngine;
        this.db = other.db;
        this.createTableSQLs = other.createTableSQLs;
        this.sql = other.sql;
        this.typeSystem = other.typeSystem;
        this.typeFactory = other.typeFactory;
        this.catalogReader = other.catalogReader;
        this.planner = other.planner;
        this.plan = other.plan;
    }

    protected SqlPlanFixture createNewInstance() {
        try {
            Constructor<? extends SqlPlanFixture> declaredConstructor =
                    this.getClass().getConstructor(this.getClass());
            return declaredConstructor.newInstance(this);
        } catch (NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public SqlPlanFixture withComputeEngine(ComputeEngine computeEngine) {
        SqlPlanFixture fixture = createNewInstance();
        fixture.computeEngine = computeEngine;
        fixture.catalogReader = new CalciteCatalogReader(CalciteSchema.from(getCalciteSchema((fixture))),
                new ArrayList<>(), typeFactory, null);
        return fixture;
    }

    public SqlPlanFixture withDb(String database) {
        SqlPlanFixture fixture = createNewInstance();
        fixture.db = database;
        return fixture;
    }

    public SqlPlanFixture withCreateTable(String createTableSQL) {
        return withMultipleCreateTables(List.of(createTableSQL));
    }

    public SqlPlanFixture withMultipleCreateTables(List<String> createTableSQLs) {
        SqlPlanFixture fixture = createNewInstance();
        fixture.createTableSQLs = createTableSQLs;
        return fixture;
    }

    public SqlPlanFixture withSql(String sql) {
        SqlPlanFixture fixture = createNewInstance();
        fixture.sql = sql;
        return fixture;
    }

    public SqlPlanFixture plan() throws Exception {
        AnalysisContext analysisContext = SqlAnalyzer.analyze(this.sql, this.computeEngine);
        this.planner = analysisContext.getPlanner();
        this.plan = analysisContext.getRelNode();
        return this;
    }

    public SqlPlanFixture plan(String expectedPlan) throws Exception {
        if (this.sql == null) {
            throw new IllegalArgumentException("withSql must be invoked.");
        }
        createNewTable();
        plan();
        assertEquals(expectedPlan, this.plan.explain());
        return this;
    }

    private SchemaPlus getCalciteSchema(SqlPlanFixture fixture) {
        switch (fixture.computeEngine) {
            case POSTGRESQL:
                return getPostgresDataSourceSchema();
            default:
                throw new UnsupportedOperationException("unsupported compute engine " + fixture.computeEngine.name() + " in getCalciteSchema");
        }
    }

    private SchemaPlus getPostgresDataSourceSchema() {
        String url = getUrl();
        DataSource dataSource = JdbcSchema.dataSource(url, driverClassName, "postgres", "postgres");
        PostgresJdbcSchemaService postgresSchemaService = new PostgresJdbcSchemaService(dataSource, typeFactory);
        ProxySchema proxySchema = new ProxySchema(List.of(), postgresSchemaService);
        KwaiCalciteSchema kwaiCalciteSchema = new KwaiCalciteSchema(null, proxySchema, EMPTY_STRING);

        try {
            Object field = FieldUtils.readDeclaredStaticField(
                    CalciteSchemaRegistry.class, "ENGINE_SCHEMA_MAP", true);
            Map<TableEngine, CalciteSchema> engineSchemaMap = (Map<TableEngine, CalciteSchema>) field;
            engineSchemaMap.put(TableEngine.POSTGRESQL, kwaiCalciteSchema);
            SchemaPlus rootSchema = kwaiCalciteSchema.plus();
            return rootSchema;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("access postgres schema error", e);
        }
    }

    private String getUrl() {
        return "jdbc:postgresql://localhost:5432/" + this.db;
    }

    private TableEngine mapComputeEngineToTableEngine(ComputeEngine computeEngine) {
        switch (computeEngine) {
            case POSTGRESQL:
                return TableEngine.POSTGRESQL;
            default:
                // 默认使用 POSTGRESQL
                return TableEngine.POSTGRESQL;
        }
    }

    public void planSql(SqlDialect sqlDialect, String expectedSql) {
        if (this.plan == null) {
            throw new IllegalArgumentException("plan() must be invoked.");
        }
        String bestPlanSql = toSql(plan, sqlDialect);
        assertEquals(expectedSql, bestPlanSql);
    }

    public String toSql(RelNode relNode, SqlDialect sqlDialect) {
        RelToSqlConverter relToSqlConverter = new RelToSqlConverter(sqlDialect);
        RelToSqlConverter.Result result = relToSqlConverter.visitRoot(relNode);
        SqlNode statement = result.asStatement();
        SqlString sqlString = statement.toSqlString(sqlDialect);
        return sqlString.getSql();
    }

    public RelNode getPlan() {
        if (this.plan == null) {
            throw new IllegalStateException("plan hasn't been created");
        }
        return this.plan;
    }

    public void createNewTable() throws SQLException {
        if (this.createTableSQLs == null || this.createTableSQLs.isEmpty()) {
            return;
        }

        switch (computeEngine) {
            case POSTGRESQL:
                createNewPostgresTables();
                break;
            default:
                throw new UnsupportedOperationException("unsupported engine " + computeEngine.name());
        }
    }

    protected void createNewPostgresTables() throws SQLException {
        String url = getUrl();

        try (Connection conn = DriverManager.getConnection(url, "postgres", "postgres")) {
            try (Statement statement = conn.createStatement()) {
                for (String createTableSQL : createTableSQLs) {
                    log.info("Executing SQL in database: {}", createTableSQL);
                    statement.execute(createTableSQL);
                }
            }
            log.info("Successfully created tables in PostgreSQL database");
        } catch (SQLException e) {
            throw new SQLException("Failed to connect to PostgreSQL instance", e);
        }
    }

    /**
     * 更新 Schema 使用真实的数据库连接
     */
    private void updateSchemaWithRealDatabase() {
        try {
            String url = getUrl();
            DataSource dataSource = JdbcSchema.dataSource(url, driverClassName, "postgres", "postgres");
            PostgresJdbcSchemaService schemaService = new PostgresJdbcSchemaService(dataSource, typeFactory);
            ProxySchema proxySchema = new ProxySchema(List.of(), schemaService);
            KwaiCalciteSchema kwaiCalciteSchema = new KwaiCalciteSchema(null, proxySchema, EMPTY_STRING);

            Object field = FieldUtils.readDeclaredStaticField(
                    CalciteSchemaRegistry.class, "ENGINE_SCHEMA_MAP", true);
            @SuppressWarnings("unchecked")
            Map<TableEngine, CalciteSchema> engineSchemaMap = (Map<TableEngine, CalciteSchema>) field;
            engineSchemaMap.put(TableEngine.POSTGRESQL, kwaiCalciteSchema);

            // 更新 catalogReader
            this.catalogReader = new CalciteCatalogReader(kwaiCalciteSchema,
                    new ArrayList<>(), typeFactory, null);

            log.info("Updated schema with real database connection");
        } catch (Exception e) {
            log.error("Failed to update schema with real database", e);
        }
    }

    /**
     * 根据列定义创建 Calcite Table
     */
    protected Table createTableFromColumns(String tableName, List<ColumnDef> columns) {
        return new AbstractTable() {
            @Override
            public RelDataType getRowType(RelDataTypeFactory typeFactory) {
                List<String> columnNames = new ArrayList<>();
                List<RelDataType> columnTypes = new ArrayList<>();

                for (ColumnDef column : columns) {
                    columnNames.add(column.name);
                    RelDataType type = mapSqlTypeToRelDataType(typeFactory, column.dataType);
                    columnTypes.add(type);
                }

                return typeFactory.createStructType(columnTypes, columnNames);
            }
        };
    }

    /**
     * 将 SQL 类型字符串映射到 RelDataType
     */
    private RelDataType mapSqlTypeToRelDataType(RelDataTypeFactory typeFactory, String sqlType) {
        String upperType = sqlType.toUpperCase().trim();

        if (upperType.startsWith("VARCHAR") || upperType.startsWith("CHARACTER VARYING")) {
            int length = extractLength(upperType, 255);
            return typeFactory.createTypeWithNullability(
                    typeFactory.createSqlType(SqlTypeName.VARCHAR, length), true);
        } else if (upperType.startsWith("CHAR")) {
            int length = extractLength(upperType, 1);
            return typeFactory.createTypeWithNullability(
                    typeFactory.createSqlType(SqlTypeName.CHAR, length), true);
        } else if (upperType.startsWith("DECIMAL") || upperType.startsWith("NUMERIC")) {
            int[] precisionScale = extractPrecisionScale(upperType);
            return typeFactory.createTypeWithNullability(
                    typeFactory.createSqlType(SqlTypeName.DECIMAL, precisionScale[0], precisionScale[1]), true);
        } else if (upperType.startsWith("INTEGER") || upperType.equals("INT")) {
            return typeFactory.createSqlType(SqlTypeName.INTEGER);
        } else if (upperType.startsWith("BIGINT")) {
            return typeFactory.createSqlType(SqlTypeName.BIGINT);
        } else if (upperType.startsWith("SMALLINT")) {
            return typeFactory.createSqlType(SqlTypeName.SMALLINT);
        } else if (upperType.startsWith("TINYINT")) {
            return typeFactory.createSqlType(SqlTypeName.TINYINT);
        } else if (upperType.startsWith("DOUBLE")) {
            return typeFactory.createSqlType(SqlTypeName.DOUBLE);
        } else if (upperType.startsWith("FLOAT") || upperType.startsWith("REAL")) {
            return typeFactory.createSqlType(SqlTypeName.FLOAT);
        } else if (upperType.startsWith("TIMESTAMP")) {
            return typeFactory.createTypeWithNullability(
                    typeFactory.createSqlType(SqlTypeName.TIMESTAMP), true);
        } else if (upperType.startsWith("DATE")) {
            return typeFactory.createTypeWithNullability(
                    typeFactory.createSqlType(SqlTypeName.DATE), true);
        } else if (upperType.startsWith("TIME")) {
            return typeFactory.createTypeWithNullability(
                    typeFactory.createSqlType(SqlTypeName.TIME), true);
        } else if (upperType.startsWith("BOOLEAN") || upperType.startsWith("BOOL")) {
            return typeFactory.createSqlType(SqlTypeName.BOOLEAN);
        } else if (upperType.startsWith("BINARY") || upperType.startsWith("VARBINARY") || upperType.startsWith("BYTEA")) {
            return typeFactory.createTypeWithNullability(
                    typeFactory.createSqlType(SqlTypeName.VARBINARY), true);
        } else if (upperType.startsWith("TEXT")) {
            return typeFactory.createTypeWithNullability(
                    typeFactory.createSqlType(SqlTypeName.VARCHAR, Integer.MAX_VALUE), true);
        } else {
            // 默认类型
            return typeFactory.createTypeWithNullability(
                    typeFactory.createSqlType(SqlTypeName.VARCHAR), true);
        }
    }

    /**
     * 提取类型长度，如 VARCHAR(50) -> 50
     */
    private int extractLength(String type, int defaultLength) {
        int start = type.indexOf('(');
        int end = type.indexOf(')');
        if (start > 0 && end > start) {
            try {
                return Integer.parseInt(type.substring(start + 1, end).trim());
            } catch (NumberFormatException e) {
                return defaultLength;
            }
        }
        return defaultLength;
    }

    /**
     * 提取精度和标度，如 DECIMAL(10, 2) -> [10, 2]
     */
    private int[] extractPrecisionScale(String type) {
        int start = type.indexOf('(');
        int end = type.indexOf(')');
        if (start > 0 && end > start) {
            try {
                String params = type.substring(start + 1, end);
                String[] parts = params.split(",");
                if (parts.length == 2) {
                    return new int[]{
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim())
                    };
                } else if (parts.length == 1) {
                    return new int[]{Integer.parseInt(parts[0].trim()), 0};
                }
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return new int[]{10, 2}; // 默认精度
    }

    /**
     * 列定义辅助类
     */
    protected static class ColumnDef {
        String name;
        String dataType;

        ColumnDef(String name, String dataType) {
            this.name = name;
            this.dataType = dataType;
        }
    }
}

