package ws.prodigy.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import ws.prodigy.FlightCacheManager;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;

public class GetCacheInfo {
    public static void register(FlightCacheManager cache) {
        get("/cache-info", (req, res) -> {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Map<String, Object>> result = new HashMap<>();

            for (String ticket : cache.listTickets()) {
                FlightCacheManager.CacheEntry entry = cache.getEntry(ticket);
                if (entry != null) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("loadedAt", entry.loadedAtMillis());
                    info.put("expiresIn", cache.getTTL(ticket));
                    result.put(ticket, info);
                }
            }

            res.type("application/json");
            return mapper.writeValueAsString(result);
        });
    }
}
