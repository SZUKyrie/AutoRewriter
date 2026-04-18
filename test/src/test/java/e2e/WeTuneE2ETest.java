package e2e;

import org.autorewriter.common.enums.ComputeEngine;
import org.autorewriter.e2e.RewritePathE2ETest;
import org.autorewriter.sql.translate.SqlTranslate;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.autorewriter.SqlTestBase.createAllTable;

public class WeTuneE2ETest extends RewritePathE2ETest {

    // PostgreSQL 方言的 app（SQL 查询使用双引号标识符，无需转换）
    private static final Set<String> PG_APPS = Set.of("wetune_discourse", "wetune_gitlab", "wetune_homeland");

    @BeforeClass
    public static void setup() {
        init();
    }

    @Test
    public void testWeTuneDiaspora() throws IOException {
        runWeTuneApp("wetune_diaspora");
    }

    @Test
    public void testWeTuneDiscourse() throws IOException {
        runWeTuneApp("wetune_discourse");
    }

    @Test
    public void testWeTuneBroadleaf() throws IOException {
        runWeTuneApp("wetune_broadleaf");
    }

    private void runWeTuneApp(String appName) throws IOException {
        Path tracesDir = Paths.get(E2E_TEST_DIR, appName, "traces");
        if (!Files.isDirectory(tracesDir)) {
            System.out.println("[SKIP] No traces dir for " + appName);
            return;
        }

        createAllTable(E2E_TEST_TABLE_DDL + appName + "/" + CUSTOM_TABLE_DDL);

        boolean isMysqlApp = !PG_APPS.contains(appName);

        List<Path> excelFiles = Files.list(tracesDir)
                .filter(p -> p.toString().endsWith(".xlsx"))
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .collect(Collectors.toList());

        for (Path excelFile : excelFiles) {
            String filename = excelFile.getFileName().toString();
            int stmtId = Integer.parseInt(filename.replace(".xlsx", ""));

            Path sqlFile = Paths.get(E2E_TEST_DIR, appName, "query", "q" + stmtId + ".sql");
            if (!Files.exists(sqlFile)) continue;

            String sql = new String(Files.readAllBytes(sqlFile)).trim();
            if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1).trim();

            // MySQL 方言的 SQL 需要先转换为 PostgreSQL 语法
            if (isMysqlApp) {
                try {
                    sql = SqlTranslate.dialectTranslate(sql, ComputeEngine.MYSQL, ComputeEngine.POSTGRESQL);
                } catch (Exception e) {
                    System.out.println("[SKIP] stmtId=" + stmtId + " dialect translate failed: " + e.getMessage());
                    continue;
                }
            }

            runManualPathTest(excelFile.toAbsolutePath().toString(), sql, appName);
        }
    }
}
