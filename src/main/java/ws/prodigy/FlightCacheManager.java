package ws.prodigy;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class FlightCacheManager {

    private final BufferAllocator allocator;
    private final long ttlMillis;
    private final QueryConfig.DatabaseConfig dbConfig;
    private final Map<String, String> queries;
    private final Map<String, CacheEntry> cache = new HashMap<>();

    public FlightCacheManager(BufferAllocator allocator, QueryConfig config, long ttlMillis) {
        this.allocator = allocator;
        this.ttlMillis = ttlMillis;
        this.dbConfig = config.database;
        this.queries = config.queries;
    }

    public VectorSchemaRoot get(String ticket) {
        CacheEntry entry = cache.get(ticket);

        if (entry == null || isExpired(entry)) {
            System.out.println("🔄 Veri yenileniyor: " + ticket);
            String sql = queries.get(ticket);

            if (sql == null) {
                throw new IllegalArgumentException("❌ Tanımsız ticket: " + ticket);
            }

            try {
                VectorSchemaRoot root = PostgresAdbcLoader2.load(allocator, dbConfig, sql);
                cache.put(ticket, new CacheEntry(root, System.currentTimeMillis()));
                return root;
            } catch (Exception e) {
                throw new RuntimeException("❌ Yükleme hatası: " + ticket, e);
            }
        }

        return entry.root;
    }

    private boolean isExpired(CacheEntry entry) {
        return System.currentTimeMillis() - entry.timestamp > ttlMillis;
    }

    private static class CacheEntry {
        final VectorSchemaRoot root;
        final long timestamp;

        CacheEntry(VectorSchemaRoot root, long timestamp) {
            this.root = root;
            this.timestamp = timestamp;
        }
    }
}

