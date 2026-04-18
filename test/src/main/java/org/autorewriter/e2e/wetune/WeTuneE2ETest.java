package org.autorewriter.e2e.wetune;

import org.autorewriter.e2e.RewritePathE2ETest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.autorewriter.SqlTestBase.createAllTable;

public class WeTuneE2ETest extends RewritePathE2ETest {

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

    /**
     * For the given app, loads all trace Excel files under {appName}/traces/,
     * finds the matching SQL file under {appName}/query/q{stmtId}.sql,
     * and runs the manual path test for each.
     */
    private void runWeTuneApp(String appName) throws IOException {
        Path tracesDir = Paths.get(E2E_TEST_DIR, appName, "traces");
        if (!Files.isDirectory(tracesDir)) {
            System.out.println("[SKIP] No traces dir for " + appName);
            return;
        }

        createAllTable(E2E_TEST_TABLE_DDL + appName + "/" + CUSTOM_TABLE_DDL);

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

            runManualPathTest(excelFile.toAbsolutePath().toString(), sql, appName);
        }
    }
}
