package ws.prodigy;

import ws.prodigy.ArrowTable;
import java.util.*;

public class ComplexMemoryBenchmark {

    public static void main(String[] args) {
        final int rowCount = 1_000_000;
        String[] names = {"Alice", "Bob", "Charlie", "Daisy", "Eve", "Frank", "Grace", "Heidi", "Ivan", "Judy"};
        String[] cities = {"New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Ankara", "Berlin", "Tokyo", "Paris", "Rome"};
        String[] types = {"STANDARD", "PREMIUM", "GUEST"};

        System.out.println("▶️ Generating diverse data for " + rowCount + " rows...");

        List<Map<String, Object>> rowBasedData = new ArrayList<>();
        Random rnd = new Random();
        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", names[rnd.nextInt(names.length)]);
            row.put("city", cities[rnd.nextInt(cities.length)]);
            row.put("active", rnd.nextBoolean());
            row.put("score", rnd.nextFloat() * 100);
            row.put("date", new Date(System.currentTimeMillis() - rnd.nextInt(1_000_000_000)));
            row.put("age", 18 + rnd.nextInt(62));
            row.put("type", types[rnd.nextInt(types.length)]);
            row.put("notes", UUID.randomUUID().toString() + " " + UUID.randomUUID().toString());
            row.put("nulls", rnd.nextInt(10) < 3 ? null : "non-null");
            rowBasedData.add(row);
        }

        Runtime runtime = Runtime.getRuntime();

        // Row-based
        System.gc();
        long beforeRow = runtime.totalMemory() - runtime.freeMemory();
        List<Map<String, Object>> rowCache = new ArrayList<>(rowBasedData);
        System.gc();
        long afterRow = runtime.totalMemory() - runtime.freeMemory();
        long rowUsage = afterRow - beforeRow;
        System.out.println("🧮 Row-based memory usage: " + (rowUsage / 1024 / 1024.0) + " MB");

        // Arrow-based
        System.gc();
        long beforeArrow = runtime.totalMemory() - runtime.freeMemory();
        ArrowTable arrow = new ArrowTable();
        arrow.addIntColumn("id");
        arrow.addStringColumn("name");
        arrow.addStringColumn("city");
        arrow.addIntColumn("active");
        arrow.addFloatColumn("score");
        arrow.addDateColumn("date");
        arrow.addIntColumn("age");
        arrow.addStringColumn("type");
        arrow.addStringColumn("notes");
        arrow.addStringColumn("nulls");

        for (Map<String, Object> row : rowBasedData) {
            arrow.appendRow(new Object[] {
                    row.get("id"),
                    row.get("name"),
                    row.get("city"),
                    Boolean.TRUE.equals(row.get("active")) ? 1 : 0,
                    row.get("score"),
                    row.get("date"),
                    row.get("age"),
                    row.get("type"),
                    row.get("notes"),
                    row.get("nulls")
            });
        }

        System.gc();
        long afterArrow = runtime.totalMemory() - runtime.freeMemory();
        long arrowUsage = afterArrow - beforeArrow;
        System.out.println("🧠 Arrow-based memory usage (JVM heap diff): " + (arrowUsage / 1024 / 1024.0) + " MB");
        System.out.println("📦 Arrow internal buffer size (accurate): " + (arrow.getTotalBufferBytes() / 1024 / 1024.0) + " MB");
    }
}