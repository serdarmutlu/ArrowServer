package ws.prodigy;

import org.apache.arrow.flight.FlightServer;
import org.apache.arrow.flight.Location;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;

import java.util.Map;

public class FlightServerApp {

    public static void main(String[] args) throws Exception {
        try (BufferAllocator allocator = new RootAllocator()) {
            String configPath = "queries.json";
            QueryConfig config = QueryConfigLoader.load(configPath);

            long ttlMillis = 5 * 60 * 1000;
            FlightCacheManager cache = new FlightCacheManager(allocator, config, ttlMillis);
            CachedFlightProducer producer = new CachedFlightProducer(cache);

            Location location = Location.forGrpcInsecure("localhost", 8815);
            FlightServer server = FlightServer.builder(allocator, location, producer)
                    .build()
                    .start();

            System.out.println("✅ Flight Server JSON'dan tam konfigürasyonla çalışıyor!");
            server.awaitTermination();
        }
    }
}
