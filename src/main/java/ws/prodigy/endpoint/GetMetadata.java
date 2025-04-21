package ws.prodigy.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import ws.prodigy.FlightCacheManager;
import ws.prodigy.QueryConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static spark.Spark.get;

public class GetMetadata {
    public static void register(FlightCacheManager cache) {
        get("/metadata", (req, res) -> {
            ObjectMapper mapper = new ObjectMapper();
            String ticket = req.queryParams("ticket");
            QueryConfig config = cache.config;
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
            result.put("initialCache", entry.initialCache);

            res.type("application/json");

            System.out.println("🎯 /metadata çağrıldı: " + ticket);
            return mapper.writeValueAsString(result);
        });

        get("/ticket-metadata-list", (req, res) -> {
            var list = new ArrayList<Map<String, Object>>();
            for (var ticket : cache.listTickets()) {
                QueryConfig config = cache.config;
                QueryConfig.QueryEntry  entry = config.queries.get(ticket);
                var map = new LinkedHashMap<String, Object>();
                map.put("ticket", ticket);
                map.put("sql", entry.sql);
                map.put("ttlMinutes", entry.ttlMinutes);
                map.put("dbType", entry.dbType);
                map.put("user", entry.user);
                map.put("password", entry.password);
                map.put("host", entry.host);
                map.put("port", entry.port);
                map.put("dbName", entry.dbName);
                map.put("initialCache", entry.initialCache);
                map.put("runtimeCache", cache.contains(ticket)); // canlı cache durumu
                list.add(map);
            }
            res.type("application/json");
            return new ObjectMapper().writeValueAsString(list);
        });

    }
}