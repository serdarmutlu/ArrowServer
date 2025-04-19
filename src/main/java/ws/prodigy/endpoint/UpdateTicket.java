package ws.prodigy.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import ws.prodigy.FlightCacheManager;
import ws.prodigy.JdbcUrlBuilder;
import ws.prodigy.MetadataRepository;
import ws.prodigy.QueryConfig;

import java.util.Map;

import static spark.Spark.put;

public class UpdateTicket {
    public static void register(FlightCacheManager cache, QueryConfig config) {
        put("/update-ticket", (req, res) -> {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> body = mapper.readValue(req.body(), Map.class);

            String ticket = (String) body.get("ticket");
            QueryConfig.QueryEntry entry = new QueryConfig.QueryEntry();
            entry.sql = (String) body.get("sql");
            entry.ttlMinutes = (int) body.get("ttl");
            entry.dbType = (String) body.get("dbType");
            entry.host = (String) body.get("host");
            entry.port = (String) body.get("port");
            entry.dbName = (String) body.get("dbName");
            entry.user = (String) body.get("user");
            entry.password = (String) body.get("password");
            entry.db = JdbcUrlBuilder.build(entry.dbType, entry.host, entry.port, entry.dbName);
            entry.cache = true;

            config.queries.put(ticket, entry);
            new MetadataRepository("metadata.db").save(ticket, entry);

            // Cache varsa sil -> yeniden yüklensin
            cache.refresh(ticket);

            res.status(200);
            return "OK";
        });
    }
}
