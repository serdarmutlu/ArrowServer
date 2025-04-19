package ws.prodigy.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import ws.prodigy.FlightCacheManager;
import ws.prodigy.QueryConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static spark.Spark.get;

public class GetTickets {
    public static void register(FlightCacheManager cache) {
        get("/ticketnames", (req, res) -> {
            ObjectMapper mapper = new ObjectMapper();
            res.type("application/json");
            return mapper.writeValueAsString(cache.listTickets());
        });

        get("/tickets", (req, res) -> {
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
                map.put("cache", entry.cache);
                map.put("initialCache", entry.initialCache);
                list.add(map);
            }
            res.type("application/json");
            return new ObjectMapper().writeValueAsString(list);
        });
    }
}