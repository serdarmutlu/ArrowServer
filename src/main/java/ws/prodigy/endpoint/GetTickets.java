package ws.prodigy.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import ws.prodigy.FlightCacheManager;

import static spark.Spark.get;

public class GetTickets {
    public static void register(FlightCacheManager cache) {
        get("/tickets", (req, res) -> {
            ObjectMapper mapper = new ObjectMapper();
            res.type("application/json");
            return mapper.writeValueAsString(cache.listTickets());
        });
    }
}