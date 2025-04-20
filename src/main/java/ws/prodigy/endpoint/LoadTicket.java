package ws.prodigy.endpoint;
import ws.prodigy.FlightCacheManager;

import static spark.Spark.post;

public class LoadTicket {
    public static void register(FlightCacheManager cache) {
        post("/ticket/load/:ticket", (req, res) -> {
            String ticket = req.params(":ticket");

            try {
                cache.load(ticket); // Cache'e al
                res.type("application/json");
                return "{\"status\":\"loaded\", \"ticket\":\"" + ticket + "\"}";
            } catch (Exception e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
            }
        });
    }
}
