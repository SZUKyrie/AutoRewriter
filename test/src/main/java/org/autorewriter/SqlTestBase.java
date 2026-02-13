package org.autorewriter;

import lombok.extern.slf4j.Slf4j;
import org.autorewriter.common.enums.ComputeEngine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Slf4j
public class SqlTestBase {

    public static Set<String> allDatabases = null;

    /**
     * 默认的 SqlPlanFixture 实例，用于创建表
     */
    protected static SqlPlanFixture sqlPlanFixtureWithHive4 = new SqlPlanFixture();

    public static void createAllTable(String absPath) {
        Map<String, List<String>> dbToDdlMap = partitionSqlByDbName(absPath);
        createTablesFromMap(dbToDdlMap);
        allDatabases = dbToDdlMap.keySet();
    }

    private static void createTablesFromMap(Map<String, List<String>> dbToDdlMap) {
        for (Map.Entry<String, List<String>> entry : dbToDdlMap.entrySet()) {
            String dbName = entry.getKey();
            List<String> createTableSqls = entry.getValue();
            SqlPlanFixture sqlPlanFixture = sqlPlanFixtureWithHive4.withComputeEngine(ComputeEngine.POSTGRESQL).withDb(dbName);
            sqlPlanFixture = sqlPlanFixture.withMultipleCreateTables(createTableSqls);
            log.info("create table with sql: {}", createTableSqls);
            try {
                sqlPlanFixture.createNewTable();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static Map<String, List<String>> partitionSqlByDbName(String absPath) {
        List<String> allQuery = readAllQuery(absPath);
        return makePartitionMap(allQuery);
    }

    private static Map<String, List<String>> makePartitionMap(List<String> allQuery) {
        Map<String, List<String>> dbToDdlMap = new HashMap<>();
        for (String sql : allQuery) {
            String dbName = extractDbNameFromSql(sql); // 提取数据库名
            if (!dbToDdlMap.containsKey(dbName)) {
                dbToDdlMap.put(dbName, new ArrayList<>());
            }
            dbToDdlMap.get(dbName).add(sql);
        }
        return dbToDdlMap;
    }

    public static List<String> readAllQuery(String absPath) {
        String fileContent = readFile(absPath);
        return makeSqlList(fileContent);
    }

    private static List<String> makeSqlList(String fileContent) {
        List<String> allQuery = new ArrayList<>();
        for (String sql : fileContent.split(";\\s*\n+")) {
            if (!sql.trim().isEmpty()) {
                allQuery.add(sql.trim());
            }
        }
        return allQuery;
    }

    public static String readFile(String absPath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(absPath))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                // pass comment in sql
                if(!currentLine.trim().startsWith("--"))
                    contentBuilder.append(currentLine).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    private static String extractDbNameFromSql(String createTableSql) {
        if (createTableSql == null) {
            return null;
        }

        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(?:`([^`]+)`|\"([^\"]+)\"|([^\\s\\.\"`]+))\\s*\\.",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE
        );
        java.util.regex.Matcher m = p.matcher(createTableSql);
        if (m.find()) {
            if (m.group(1) != null) return m.group(1);
            if (m.group(2) != null) return m.group(2);
            return m.group(3);
        }

        // 回退：取第一个点前的部分，去除两端的标点/空白/引号
        int idx = createTableSql.indexOf('.');
        if (idx > 0) {
            String left = createTableSql.substring(0, idx).trim();
            // 去掉开头/结尾的标点和空白
            left = left.replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "");
            // 额外去掉包裹的反引号或双引号
            if (left.length() >= 2 && left.startsWith("`") && left.endsWith("`")) {
                left = left.substring(1, left.length() - 1);
            } else if (left.length() >= 2 && left.startsWith("\"") && left.endsWith("\"")) {
                left = left.substring(1, left.length() - 1);
            }
            if (!left.isEmpty()) return left;
        }

        return null;
    }
}
