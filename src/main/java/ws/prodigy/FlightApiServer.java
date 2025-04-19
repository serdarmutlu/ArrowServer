package ws.prodigy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.arrow.memory.BufferAllocator;
import ws.prodigy.endpoint.*;

import static spark.Spark.*;

public class FlightApiServer {
    public static void start(FlightCacheManager cacheManager, BufferAllocator allocator, QueryConfig config) {
        port(8080);
        staticFiles.location("/static");

        ObjectMapper mapper = new ObjectMapper();

        // Ana sayfayı yönlendir
        get("/", (req, res) -> {
            res.redirect("/flight_ticket_table_rebuilt.html");
            return null;
        });

        AddTicket.register(cacheManager, new MetadataRepository("metadata.db"));
        UpdateTicket.register(cacheManager, config);
        DeleteTicket.register(cacheManager, config);
        GetTickets.register(cacheManager);
        GetMetadata.register(config);
        GetData.register(cacheManager, config);
        GetCacheInfo.register(cacheManager);
        SqlQuery.register(cacheManager);
    }
}
