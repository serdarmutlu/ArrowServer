package ws.prodigy;

import org.apache.arrow.adbc.core.*;
import org.apache.arrow.adbc.driver.jdbc.JdbcDriver;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.types.pojo.Schema;

import java.util.HashMap;
import java.util.Map;

public class GenericAdbcLoader {
    public static VectorSchemaRoot load(BufferAllocator allocator, QueryConfig.QueryEntry entry) throws Exception {
        String jdbcUrl = JdbcUrlBuilder.build(entry.dbType, entry.host, entry.port, entry.dbName);

        System.out.println("\n🛠️ [ADBC LOADER] Ticket SQL: " + entry.sql);
        System.out.println("🔗 JDBC URL: " + jdbcUrl);
        System.out.println("👤 User: " + entry.user);

        Map<String, Object> options = new HashMap<>();
        AdbcDriver.PARAM_URI.set(options, jdbcUrl);
        AdbcDriver.PARAM_USERNAME.set(options, entry.user);
        AdbcDriver.PARAM_PASSWORD.set(options, entry.password);

        try (AdbcDatabase db = new JdbcDriver(allocator).open(options);
             AdbcConnection conn = db.connect();
             AdbcStatement stmt = conn.createStatement()) {

            stmt.setSqlQuery(entry.sql);

            try (AdbcStatement.QueryResult result = stmt.executeQuery();
                 ArrowReader reader = result.getReader()) {

                System.out.println("📥 ArrowReader oluşturuldu: " + reader.getClass().getName());

                Schema schema = reader.getVectorSchemaRoot().getSchema();
                System.out.println("📋 Schema alındı: " + schema);

                VectorSchemaRoot combined = VectorSchemaRoot.create(schema, allocator);
                combined.allocateNew();
                System.out.println("🧠 Boş VectorSchemaRoot oluşturuldu ve allocate edildi");

                int totalRows = 0;
                boolean batchFound = false;

                try {
                    while (reader.loadNextBatch()) {
                        System.out.println("✅ batch yüklendi!");
                        batchFound = true;
                        VectorSchemaRoot batch = reader.getVectorSchemaRoot();
                        int batchSize = batch.getRowCount();

                        for (int row = 0; row < batchSize; row++) {
                            for (int col = 0; col < batch.getFieldVectors().size(); col++) {
                                var src = batch.getVector(col);
                                var dst = combined.getVector(col);
                                dst.copyFromSafe(row, totalRows, src);
                            }
                            totalRows++;
                        }
                        combined.setRowCount(totalRows);
                    }
                } catch (Exception e) {
                    System.out.println("❌ batch okuma sırasında hata: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }

                if (!batchFound) {
                    System.out.println("⚠️ hiç batch yüklenmedi.");
                } else {
                    System.out.println("✅ ADBC yüklemesi tamam: " + totalRows + " satır birleştirildi.");
                }

                return combined;
            }
        } catch (Exception e) {
            System.out.println("❌ Genel hata oluştu: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
