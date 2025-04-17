package ws.prodigy;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Request;
import spark.Response;

import java.util.Map;

import static spark.Spark.post;

public class AddTicketEndpoint {

    public static void register(FlightCacheManager cache, MetadataRepository repository) {

        post("/add-ticket", (Request req, Response res) -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> body = mapper.readValue(req.body(), Map.class);

                String ticket = (String) body.get("ticket");
                String sql = (String) body.get("sql");
                String dbType = (String) body.get("dbType");
                String host = (String) body.get("host");
                String port = (String) body.get("port");
                String dbName = (String) body.get("dbName");
                String user = (String) body.get("user");
                String password = (String) body.get("password");

                String jdbcUrl = JdbcUrlBuilder.build(dbType, host, port, dbName);

                QueryConfig.QueryEntry entry = new QueryConfig.QueryEntry();
                entry.sql = sql;
                entry.cache = true;
                entry.ttlMinutes = 60;
                entry.db = jdbcUrl;
                entry.user = user;
                entry.password = password;

                repository.save(ticket, entry, jdbcUrl, user, password);
                cache.config.queries.put(ticket, entry);
                cache.loadIfMissing(ticket);

                res.type("text/plain");
                return "OK";

            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "ERROR: " + e.getMessage();
            }
        });
    }
}
