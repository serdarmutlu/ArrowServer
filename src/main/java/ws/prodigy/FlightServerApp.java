package ws.prodigy;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.flight.FlightServer;
import org.apache.arrow.flight.Location;

import java.util.HashMap;
import java.util.List;

public class FlightServerApp {
    public static void main(String[] args) throws Exception {
        BufferAllocator allocator = new RootAllocator();

        // Metadata'dan config üret
        MetadataRepository repository = new MetadataRepository("metadata.db");
        repository.init();
        List<MetadataRepository.TableMetadata> metadataList = repository.loadAll();

        QueryConfig config = new QueryConfig();
        config.queries = new HashMap<>();

        for (MetadataRepository.TableMetadata meta : metadataList) {
            QueryConfig.QueryEntry entry = new QueryConfig.QueryEntry();
            entry.sql = meta.sql();
            entry.cache = meta.autoCache();
            entry.ttlMinutes = (int) (meta.ttlMillis() / 60000); // millis → minutes
            entry.dbType = meta.dbType();
            entry.host = meta.host();
            entry.port = meta.port();
            entry.dbName = meta.dbName();
            entry.user = meta.user();
            entry.password = meta.password();
            entry.db = JdbcUrlBuilder.build(entry.dbType, entry.host, entry.port, entry.dbName);

            config.queries.put(meta.ticket(), entry);
        }

        FlightCacheManager cache = new FlightCacheManager(allocator, config);

        // Flight Server başlat
        CachedFlightProducer producer = new CachedFlightProducer(cache);
        Location location = Location.forGrpcInsecure("localhost", 8815);
        FlightServer server = FlightServer.builder(allocator, location, producer).build().start();

        // API başlat
        FlightApiServer.start(cache, allocator, config);


        server.awaitTermination();
    }
}
