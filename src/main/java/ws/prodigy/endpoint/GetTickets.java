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
        get("/ticket/names", (req, res) -> {
            ObjectMapper mapper = new ObjectMapper();
            res.type("application/json");
            return mapper.writeValueAsString(cache.listTickets());
        });

        get("/ticket/all", (req, res) -> {
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