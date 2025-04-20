package ws.prodigy.endpoint;

import ws.prodigy.FlightCacheManager;
import static spark.Spark.post;

public class UnloadTicket {
    public static void register(FlightCacheManager cache) {
        post("/ticket/unload/:ticket", (req, res) -> {
            String ticket = req.params(":ticket");
            cache.unload(ticket);
            res.type("application/json");
            return "{\"status\":\"unloaded\", \"ticket\":\"" + ticket + "\"}";
        });
    }
}
