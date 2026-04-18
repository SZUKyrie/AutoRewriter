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
    public static SqlPlanFixture sqlPlanFixture = new SqlPlanFixture();

    public static void createAllTable(String absPath) {
        Map<String, List<String>> dbToDdlMap = partitionSqlByDbName(absPath);
        createTablesFromMap(dbToDdlMap);
        allDatabases = dbToDdlMap.keySet();
    }

    private static void createTablesFromMap(Map<String, List<String>> dbToDdlMap) {
        for (Map.Entry<String, List<String>> entry : dbToDdlMap.entrySet()) {
            String dbName = entry.getKey();
            if (dbName == null) {
                log.warn("Skipping {} DDL statements with no schema prefix", entry.getValue().size());
                continue;
            }
            List<String> createTableSqls = entry.getValue();
            SqlPlanFixture sqlPlanFixture = SqlTestBase.sqlPlanFixture.withDb(dbName).withComputeEngine(ComputeEngine.POSTGRESQL);
            sqlPlanFixture = sqlPlanFixture.withMultipleCreateTables(createTableSqls);
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
            String[] result = extractDbNameAndStripPrefix(sql);
            String dbName = result[0];
            String strippedSql = result[1];
            if (!dbToDdlMap.containsKey(dbName)) {
                dbToDdlMap.put(dbName, new ArrayList<>());
            }
            dbToDdlMap.get(dbName).add(strippedSql);
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
                if(!currentLine.trim().startsWith("--"))
                    contentBuilder.append(currentLine).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    /**
     * 从 CREATE TABLE / DROP TABLE / CREATE INDEX 等语句中提取 schema/db 前缀，
     * 同时返回去掉该前缀后的 SQL（避免 PostgreSQL 把它识别为 schema 限定符）。
     *
     * @return String[2]：[0] = dbName，[1] = 去掉前缀的 SQL
     */
    private static String[] extractDbNameAndStripPrefix(String sql) {
        if (sql == null) {
            return new String[]{null, null};
        }

        // 匹配 `schema`. 或 "schema". 或 schema. 形式，全局替换所有出现位置
        java.util.regex.Pattern prefixPattern = java.util.regex.Pattern.compile(
                "(?:`([^`]+)`|\"([^\"]+)\"|([A-Za-z_][A-Za-z0-9_]*))\\.",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher m = prefixPattern.matcher(sql);

        String dbName = null;
        // 取第一个匹配到的前缀作为 dbName
        if (m.find()) {
            if (m.group(1) != null) dbName = m.group(1);
            else if (m.group(2) != null) dbName = m.group(2);
            else dbName = m.group(3);
        }

        // 去掉所有 schema. 前缀（包含反引号/双引号形式）
        String strippedSql = prefixPattern.matcher(sql).replaceAll("");

        return new String[]{dbName, strippedSql};
    }
}
