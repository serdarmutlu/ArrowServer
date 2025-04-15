package ws.prodigy;

import org.apache.arrow.adbc.core.AdbcConnection;
import org.apache.arrow.adbc.core.AdbcDatabase;
import org.apache.arrow.adbc.core.AdbcDriver;
import org.apache.arrow.adbc.core.AdbcStatement;
import org.apache.arrow.adbc.driver.jdbc.JdbcDriver;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PostgresAdbcLoader_x {

    public static void loadData() throws Exception {

        final String jdbcUrl = "jdbc:postgresql://localhost:5432/serdar";
        final Map<String, Object> parameters = new HashMap<>();
        AdbcDriver.PARAM_URI.set(parameters, jdbcUrl);
        AdbcDriver.PARAM_USERNAME.set(parameters, "serdar");
        AdbcDriver.PARAM_PASSWORD.set(parameters, "serdar");

        try (
            BufferAllocator allocator = new RootAllocator();
            AdbcDatabase db = new JdbcDriver(allocator).open(parameters);
            AdbcConnection conn = db.connect();
            AdbcStatement stmt = conn.createStatement()
            )
            {
                stmt.setSqlQuery("SELECT id, name FROM customers");

                try (AdbcStatement.QueryResult result = stmt.executeQuery();
                     ArrowReader reader = result.getReader();
                     VectorSchemaRoot root = reader.getVectorSchemaRoot()) {

                    while (reader.loadNextBatch()) {
                        IntVector idVec = (IntVector) root.getVector("id");
                        VarCharVector nameVec = (VarCharVector) root.getVector("name");

                        for (int i = 0; i < root.getRowCount(); i++) {
                            int id = idVec.get(i);
                            String name = new String(nameVec.get(i), StandardCharsets.UTF_8);
                            System.out.println("id = " + id + ", name = " + name);
                        }
                    }
                }
            }
    }

    public static void main(String[] args) throws Exception {
            loadData();
    }
}
