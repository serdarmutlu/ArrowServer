package ws.prodigy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.arrow.memory.BufferAllocator;
import ws.prodigy.endpoint.*;

import static spark.Spark.*;

public class FlightApiServer {
    public static void start(FlightCacheManager cache, BufferAllocator allocator, QueryConfig config) {
        port(8080);
        staticFiles.location("/static");

        ObjectMapper mapper = new ObjectMapper();

        // Ana sayfayı yönlendir
        get("/", (req, res) -> {
            res.redirect("/flight_ticket_table_rebuilt.html");
            return null;
        });

        AddTicket.register(cache, new MetadataRepository("metadata.db"));
        UpdateTicket.register(cache, config);
        DeleteTicket.register(cache, config);
        GetTickets.register(cache);
        GetMetadata.register(config);
        GetData.register(cache, config);
        GetCacheInfo.register(cache);
        SqlQuery.register(cache);
    }
}
