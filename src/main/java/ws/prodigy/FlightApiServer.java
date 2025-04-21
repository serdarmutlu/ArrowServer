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
            res.redirect("/index.html");
            return null;
        });

        AddTicket.register(cacheManager, new MetadataRepository("metadata.db"));
        UpdateTicket.register(cacheManager);
        DeleteTicket.register(cacheManager);

        GetData.register(cacheManager);
        GetCacheInfo.register(cacheManager);
        GetMetadata.register(cacheManager);
        GetTickets.register(cacheManager);

        UnloadTicket.register(cacheManager);
        LoadTicket.register(cacheManager);
        GetCacheMemory.register(cacheManager);

        SqlQuery.register(cacheManager);
    }
}
