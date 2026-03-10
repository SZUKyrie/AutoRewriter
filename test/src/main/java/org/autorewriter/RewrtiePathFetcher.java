package org.autorewriter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class RewrtiePathFetcher {

    private static final String SHEET_NAME = "All_Trace_Entries";
    private static final String[] HEADER = {"#", "Rule ID", "Rule Description", "Source Plan", "Target Plan"};

    private static class TraceEntry {
        final String seq;
        final String ruleId;
        final String ruleDesc;
        final String sourcePlan;
        final String targetPlan;

        TraceEntry(String seq, String ruleId, String ruleDesc, String sourcePlan, String targetPlan) {
            this.seq = seq;
            this.ruleId = ruleId;
            this.ruleDesc = ruleDesc;
            this.sourcePlan = sourcePlan;
            this.targetPlan = targetPlan;
        }
    }

    private static class PathState {
        final String currentPlan;
        final List<TraceEntry> path;

        PathState(String currentPlan, List<TraceEntry> path) {
            this.currentPlan = currentPlan;
            this.path = path;
        }
    }

    public void buildRewritePaths(String inputPath, String outputPath) throws IOException {
        List<TraceEntry> entries = new ArrayList<>();
        String originalPlan;

        // 1. Read All_Trace_Entries sheet
        try (FileInputStream fis = new FileInputStream(inputPath);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet '" + SHEET_NAME + "' not found in " + inputPath);
            }

            originalPlan = readEntries(sheet, entries);
        }

        if (originalPlan == null) {
            throw new IllegalStateException("No 'Original' row found in the sheet");
        }

        // 2. Build a map: sourcePlan -> list of entries
        Map<String, List<TraceEntry>> sourcePlanIndex = new HashMap<>();
        for (TraceEntry entry : entries) {
            if (entry.sourcePlan != null) {
                sourcePlanIndex.computeIfAbsent(entry.sourcePlan, k -> new ArrayList<>()).add(entry);
            }
        }

        // 3. BFS to find all root-to-leaf paths
        List<List<TraceEntry>> allPaths = findAllPaths(originalPlan, sourcePlanIndex);

        // 4. Write output Excel: one worksheet per path
        writeOutput(outputPath, originalPlan, allPaths);

        System.out.println("Found " + allPaths.size() + " rewrite paths. Output written to: " + outputPath);
    }

    private String readEntries(Sheet sheet, List<TraceEntry> entries) {
        String originalPlan = null;
        boolean headerSkipped = false;

        for (Row row : sheet) {
            if (!headerSkipped) {
                headerSkipped = true;
                continue;
            }

            String seq = getCellString(row, 0);
            String ruleId = getCellString(row, 1);
            String ruleDesc = getCellString(row, 2);
            String sourcePlan = getCellString(row, 3);
            String targetPlan = getCellString(row, 4);

            if ("Original".equals(seq)) {
                originalPlan = sourcePlan;
                continue;
            }

            entries.add(new TraceEntry(seq, ruleId, ruleDesc, sourcePlan, targetPlan));
        }

        return originalPlan;
    }

    private List<List<TraceEntry>> findAllPaths(String originalPlan, Map<String, List<TraceEntry>> sourcePlanIndex) {
        List<List<TraceEntry>> allPaths = new ArrayList<>();
        Deque<PathState> queue = new ArrayDeque<>();

        List<TraceEntry> firstSteps = sourcePlanIndex.getOrDefault(originalPlan, Collections.emptyList());
        for (TraceEntry entry : firstSteps) {
            List<TraceEntry> path = new ArrayList<>();
            path.add(entry);
            queue.add(new PathState(entry.targetPlan, path));
        }

        if (firstSteps.isEmpty()) {
            allPaths.add(Collections.emptyList());
            return allPaths;
        }

        while (!queue.isEmpty()) {
            PathState state = queue.poll();
            List<TraceEntry> nextEntries = sourcePlanIndex.getOrDefault(state.currentPlan, Collections.emptyList());

            if (nextEntries.isEmpty()) {
                allPaths.add(state.path);
            } else {
                for (TraceEntry next : nextEntries) {
                    List<TraceEntry> extendedPath = new ArrayList<>(state.path);
                    extendedPath.add(next);
                    queue.add(new PathState(next.targetPlan, extendedPath));
                }
            }
        }

        return allPaths;
    }

    private void writeOutput(String outputPath, String originalPlan, List<List<TraceEntry>> allPaths) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            for (int i = 0; i < allPaths.size(); i++) {
                List<TraceEntry> path = allPaths.get(i);
                Sheet sheet = wb.createSheet("Path_" + i);

                int rowIdx = 0;

                // Header row
                Row headerRow = sheet.createRow(rowIdx++);
                for (int c = 0; c < HEADER.length; c++) {
                    headerRow.createCell(c).setCellValue(HEADER[c]);
                }

                // Original row (no rule applied)
                Row origRow = sheet.createRow(rowIdx++);
                origRow.createCell(0).setCellValue("Original");
                origRow.createCell(3).setCellValue(originalPlan);

                // Step rows
                for (int step = 0; step < path.size(); step++) {
                    TraceEntry entry = path.get(step);
                    Row stepRow = sheet.createRow(rowIdx++);
                    stepRow.createCell(0).setCellValue(step + 1);
                    setCellValue(stepRow, 1, entry.ruleId);
                    setCellValue(stepRow, 2, entry.ruleDesc);
                    setCellValue(stepRow, 3, entry.sourcePlan);
                    setCellValue(stepRow, 4, entry.targetPlan);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                wb.write(fos);
            }
        }
    }

    private String getCellString(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (type == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        } else if (type == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (type == CellType.BLANK) {
            return null;
        }
        return cell.toString();
    }

    private void setCellValue(Row row, int colIndex, String value) {
        if (value != null) {
            row.createCell(colIndex).setCellValue(value);
        }
    }

    public static void main(String[] args) throws IOException {
        String inputPath;
        String outputPath;
        if (args.length >= 2) {
            inputPath = args[0];
            outputPath = args[1];
        } else if (args.length == 1) {
            inputPath = args[0];
            outputPath = getResourcesDir() + "/diaspora_rewrite_paths.xlsx";
        } else {
            System.err.println("Usage: RewrtiePathFetcher <input.xlsx> [output.xlsx]");
            System.exit(1);
            return;
        }
        new RewrtiePathFetcher().buildRewritePaths(inputPath, outputPath);
    }

    private static String getResourcesDir() {
        String projectDir = System.getProperty("user.dir");
        // If run from project root, target test/src/main/resources
        String resourcesDir = projectDir + "/test/src/main/resources";
        java.io.File dir = new java.io.File(resourcesDir);
        if (!dir.exists()) {
            // If run from within the test module
            resourcesDir = projectDir + "/src/main/resources";
        }
        return resourcesDir;
    }
}
