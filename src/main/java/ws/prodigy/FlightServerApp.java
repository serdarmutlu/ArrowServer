package ws.prodigy;

import org.apache.arrow.flight.FlightServer;
import org.apache.arrow.flight.Location;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;


public class FlightServerApp {
    public static void main(String[] args) throws Exception {
        BufferAllocator allocator = new RootAllocator();
        QueryConfig config = QueryConfigLoader.load("queries.json");

        FlightCacheManager cache = new FlightCacheManager(allocator, config);

        // Flight Server başlat
        CachedFlightProducer producer = new CachedFlightProducer(cache);
        Location location = Location.forGrpcInsecure("localhost", 8815);
        FlightServer server = FlightServer.builder(allocator, location, producer).build().start();

        // API başlat
        FlightApiServer.start(cache, allocator); // 🎯 cache dışarıdan geliyor

        server.awaitTermination();
    }
}

