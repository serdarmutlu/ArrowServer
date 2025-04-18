package ws.prodigy.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import ws.prodigy.QueryConfig;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;

public class GetMetadata {
    public static void register(QueryConfig config) {
        get("/metadata", (req, res) -> {
            ObjectMapper mapper = new ObjectMapper();
            String ticket = req.queryParams("ticket");
            QueryConfig.QueryEntry entry = config.queries.get(ticket);

            if (entry == null) {
                res.status(404);
                return "Ticket bulunamadı";
            }

            Map<String, Object> result = new HashMap<>();
            result.put("ticket", ticket);
            result.put("sql", entry.sql);
            result.put("ttlMinutes", entry.ttlMinutes);
            result.put("dbType", entry.dbType);
            result.put("host", entry.host);
            result.put("port", entry.port);
            result.put("dbName", entry.dbName);
            result.put("user", entry.user);
            result.put("password", entry.password);
            result.put("cache", entry.cache);
            result.put("initialCache", entry.initialCache);

            res.type("application/json");

            System.out.println("🎯 /metadata çağrıldı: " + ticket);
            return mapper.writeValueAsString(result);
        });
    }
}