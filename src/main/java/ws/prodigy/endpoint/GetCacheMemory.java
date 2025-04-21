package ws.prodigy.endpoint;
import ws.prodigy.FlightCacheManager;

import static spark.Spark.get;

public class GetCacheMemory {
    public static void register(FlightCacheManager cache) {
        get("/cache/memory", (req, res) -> {
            long bytes = cache.getTotalBufferBytes();
            res.type("application/json");
            return "{\"memoryBytes\":" + bytes + "}";
        });
    }
}