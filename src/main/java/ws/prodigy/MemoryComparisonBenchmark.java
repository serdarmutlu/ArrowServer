package ws.prodigy;

import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import ws.prodigy.ArrowTable;

import java.util.*;

public class MemoryComparisonBenchmark {

    public static void main(String[] args) {
        final int rowCount = 1_000_000;

        System.out.println("▶️ Generating data for " + rowCount + " rows...");

        List<Map<String, Object>> rowBasedData = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("status", i % 2 == 0 ? "ACTIVE" : "INACTIVE");
            row.put("amount", i * 0.5);
            rowBasedData.add(row);
        }

        Runtime runtime = Runtime.getRuntime();

        // Row-based measurement
        System.gc();
        long beforeRow = runtime.totalMemory() - runtime.freeMemory();
        List<Map<String, Object>> rowCache = new ArrayList<>(rowBasedData);
        System.gc();
        long afterRow = runtime.totalMemory() - runtime.freeMemory();
        long rowUsage = afterRow - beforeRow;

        System.out.println("🧮 Row-based memory usage: " + (rowUsage / 1024 / 1024.0) + " MB");

        // Arrow-based measurement
        System.gc();
        long beforeArrow = runtime.totalMemory() - runtime.freeMemory();

        ArrowTable arrow = new ArrowTable();
        arrow.addIntColumn("id");
        arrow.addStringColumn("status");
        arrow.addFloatColumn("amount");

        for (Map<String, Object> row : rowBasedData) {
            arrow.appendRow(new Object[]{
                    row.get("id"),
                    row.get("status"),
                    row.get("amount")
            });
        }

        System.gc();
        long afterArrow = runtime.totalMemory() - runtime.freeMemory();
        long arrowUsage = afterArrow - beforeArrow;

        System.out.println("📦 Arrow internal buffer size (accurate): " + (arrow.getTotalBufferBytes() / 1024 / 1024.0) + " MB");

    }
}