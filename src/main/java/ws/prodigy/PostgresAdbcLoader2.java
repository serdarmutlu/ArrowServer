package ws.prodigy;

import org.apache.arrow.adbc.core.AdbcConnection;
import org.apache.arrow.adbc.core.AdbcDatabase;
import org.apache.arrow.adbc.core.AdbcDriver;
import org.apache.arrow.adbc.core.AdbcStatement;
import org.apache.arrow.adbc.driver.jdbc.JdbcDriver;
import org.apache.arrow.memory.*;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;

import java.util.HashMap;
import java.util.Map;

public class PostgresAdbcLoader2 {

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
        ArrowReader reader = result.getReader();
        VectorSchemaRoot root = reader.getVectorSchemaRoot();

        if (reader.loadNextBatch()) {
            return root;
        } else {
            throw new IllegalStateException("Veri yüklenemedi.");
        }
    }



}
