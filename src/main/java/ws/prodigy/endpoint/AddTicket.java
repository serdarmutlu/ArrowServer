package ws.prodigy.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Request;
import spark.Response;
import ws.prodigy.FlightCacheManager;
import ws.prodigy.JdbcUrlBuilder;
import ws.prodigy.MetadataRepository;
import ws.prodigy.QueryConfig;

import java.util.Map;

import static spark.Spark.post;

public class AddTicket {

    public static void register(FlightCacheManager cacheManager, MetadataRepository repository) {

        post("/ticket/add", (Request req, Response res) -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> body = mapper.readValue(req.body(), Map.class);

                String ticket = (String) body.get("ticket");
                String sql = (String) body.get("sql");
                String dbType = (String) body.get("dbType");
                String host = (String) body.get("host");
                String port = String.valueOf(body.get("port"));
                String dbName = (String) body.get("dbName");
                String user = (String) body.get("user");
                String password = (String) body.get("password");
                String ttl = String.valueOf(body.get("ttl"));

                QueryConfig.QueryEntry entry = new QueryConfig.QueryEntry();
                entry.sql = sql;
                entry.cache = true;
                entry.ttlMinutes = Integer.parseInt(ttl);
                entry.dbType = dbType;
                entry.host = host;
                entry.port = port;
                entry.dbName = dbName;
                entry.user = user;
                entry.password = password;
                entry.db = JdbcUrlBuilder.build(dbType, host, port, dbName);

                repository.save(ticket, entry);
                cacheManager.config.queries.put(ticket, entry);
                cacheManager.loadIfMissing(ticket);

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