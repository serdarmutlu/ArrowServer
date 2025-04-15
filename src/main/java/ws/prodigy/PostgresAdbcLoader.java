package ws.prodigy;

import org.apache.arrow.adbc.core.AdbcConnection;
import org.apache.arrow.adbc.core.AdbcDatabase;
import org.apache.arrow.adbc.core.AdbcDriver;
import org.apache.arrow.adbc.core.AdbcStatement;
import org.apache.arrow.adbc.driver.jdbc.JdbcDriver;
import org.apache.arrow.memory.*;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;

import java.util.HashMap;
import java.util.Map;

public class PostgresAdbcLoader {
//    public static VectorSchemaRoot loadBatches(BufferAllocator allocator, QueryConfig.DatabaseConfig db, String sql) throws Exception {
//        final Map<String, Object> parameters = new HashMap<>();
//        String url = "jdbc:postgresql://" + db.host + ":" + db.port + "/" + db.dbName;
//
//        AdbcDriver.PARAM_URI.set(parameters, url);
//        AdbcDriver.PARAM_USERNAME.set(parameters, db.user);
//        AdbcDriver.PARAM_PASSWORD.set(parameters, db.password);
//
//        JdbcDriver driver = new JdbcDriver(allocator);
//        AdbcDatabase database = driver.open(parameters);
//        AdbcConnection conn = database.connect();
//        AdbcStatement stmt = conn.createStatement();
//
//        stmt.setSqlQuery(sql);
//        AdbcStatement.QueryResult result = stmt.executeQuery();
//
//        try (ArrowReader reader = result.getReader()) {
//            reader.loadNextBatch(); // ilk batch'i yükle
//            VectorSchemaRoot combinedRoot = VectorSchemaRoot.create(reader.getVectorSchemaRoot().getSchema(), allocator);
//            combinedRoot.allocateNew();
//
//            int totalRows = 0;
//
//            do {
//                VectorSchemaRoot batch = reader.getVectorSchemaRoot();
//                int rowCount = batch.getRowCount();
//
//                for (int col = 0; col < batch.getFieldVectors().size(); col++) {
//                    FieldVector source = batch.getFieldVectors().get(col);
//                    FieldVector target = combinedRoot.getVector(col);
//
//                    for (int row = 0; row < rowCount; row++) {
//                        target.copyFromSafe(row, totalRows + row, source);
//                    }
//                }
//
//                totalRows += rowCount;
//
//            } while (reader.loadNextBatch());
//
//            combinedRoot.setRowCount(totalRows);
//            return combinedRoot;
//        }
//    }


    public static VectorSchemaRoot load(BufferAllocator allocator, QueryConfig.DatabaseConfig db, String sql) throws Exception {
        final Map<String, Object> parameters = new HashMap<>();
        String url = "jdbc:postgresql://" + db.host + ":" + db.port + "/" + db.dbName;

        AdbcDriver.PARAM_URI.set(parameters, url);
        AdbcDriver.PARAM_USERNAME.set(parameters, db.user);
        AdbcDriver.PARAM_PASSWORD.set(parameters, db.password);

        JdbcDriver driver = new JdbcDriver(allocator);
        AdbcDatabase database = driver.open(parameters);
        AdbcConnection conn = database.connect();
        AdbcStatement stmt = conn.createStatement();

        stmt.setSqlQuery(sql);
        AdbcStatement.QueryResult result = stmt.executeQuery();

        try (ArrowReader reader = result.getReader()) {
            VectorSchemaRoot sourceRoot = reader.getVectorSchemaRoot();
            VectorSchemaRoot combinedRoot = VectorSchemaRoot.create(sourceRoot.getSchema(), allocator);
            combinedRoot.allocateNew();

            int totalRows = 0;

            while (reader.loadNextBatch()) {
                int rowCount = sourceRoot.getRowCount();

                for (int col = 0; col < sourceRoot.getFieldVectors().size(); col++) {
                    var source = sourceRoot.getVector(col);
                    var target = combinedRoot.getVector(col);

                    for (int i = 0; i < rowCount; i++) {
                        target.copyFromSafe(i, totalRows + i, source);
                    }
                }

                totalRows += rowCount;
            }

            combinedRoot.setRowCount(totalRows);
            return combinedRoot;
        }
    }



}
