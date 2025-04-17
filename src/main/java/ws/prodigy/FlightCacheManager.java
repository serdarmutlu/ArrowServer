package ws.prodigy;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;

import java.util.HashMap;
import java.util.Map;

import java.util.Set;




public class FlightCacheManager {

    private final QueryConfig.DatabaseConfig dbConfig;
    private final Map<String, QueryConfig.QueryEntry> queryEntries;
    private final BufferAllocator allocator;
    final QueryConfig config; // işte bu eksikti

    private final Map<String, CacheEntry> cache = new HashMap<>(); // ✅ burada

    public FlightCacheManager(BufferAllocator allocator, QueryConfig config) {
        this.allocator = allocator;
        this.config = config;
        this.dbConfig = config.database;
        this.queryEntries = config.queries;

        preloadCache();
    }

    private void preloadCache() {
        for (Map.Entry<String, QueryConfig.QueryEntry> entry : queryEntries.entrySet()) {
            String ticket = entry.getKey();
            QueryConfig.QueryEntry query = entry.getValue();

            if (query.cache) {
                System.out.println("🔁 Önceden yükleniyor: " + ticket);
                try {
                    VectorSchemaRoot root = PostgresAdbcLoader.load(allocator, dbConfig, query.sql);
                    cache.put(ticket, new CacheEntry(root, System.currentTimeMillis()));
                    System.out.println("✅ " + ticket + " cache'e alındı (" + root.getRowCount() + " satır)");
                } catch (Exception e) {
                    throw new RuntimeException("Yenileme hatası: " + ticket, e);
                }
            }
        }
    }

    public void loadIfMissing(String ticket) {
        QueryConfig.QueryEntry query = queryEntries.get(ticket);
        if (query == null) throw new IllegalArgumentException("❌ Tanımsız ticket: " + ticket);

        CacheEntry entry = cache.get(ticket);
        long now = System.currentTimeMillis();
        long ttlMillis = query.ttlMinutes * 60_000L;

        boolean expired = entry == null || (now - entry.loadedAt > ttlMillis);

        if (expired) {
            try {
                System.out.println("♻️ TTL yenileme veya ilk yükleme: " + ticket);
                VectorSchemaRoot root = PostgresAdbcLoader.load(allocator, dbConfig, query.sql);

                if (entry != null && entry.root != null) {
                    entry.root.close(); // önceki root'u temizle
                }

                cache.put(ticket, new CacheEntry(root, now));
            } catch (Exception e) {
                throw new RuntimeException("Yenileme hatası: " + ticket, e);
            }
        }
    }

    public VectorSchemaRoot get(String ticket) {
        loadIfMissing(ticket);
        return cache.get(ticket).root;
    }

    public Set<String> listTickets() {
        return queryEntries.keySet();
    }

    static class CacheEntry {
        final VectorSchemaRoot root;
        final long loadedAt;

        CacheEntry(VectorSchemaRoot root, long loadedAt) {
            this.root = root;
            this.loadedAt = loadedAt;
        }
    }

    public long getLoadedAt(String ticket) {
        CacheEntry entry = cache.get(ticket);
        return (entry != null) ? entry.loadedAt : -1;
    }

    public long getExpiresIn(String ticket) {
        QueryConfig.QueryEntry query = queryEntries.get(ticket);
        CacheEntry entry = cache.get(ticket);

        if (entry == null || query == null) return -1;

        long now = System.currentTimeMillis();
        long ttlMs = query.ttlMinutes * 60_000L;
        long age = now - entry.loadedAt;
        return Math.max(0, ttlMs - age);
    }

    public void load(String ticket, String sql, String dbUrl, String dbUser, String dbPass, long ttlMillis) {
        QueryConfig.QueryEntry entry = new QueryConfig.QueryEntry();
        entry.sql = sql;
        entry.cache = true;
        entry.ttlMinutes = (int) (ttlMillis / 60000);

        if (config.queries == null) {
            config.queries = new HashMap<>();
        }
        config.queries.put(ticket, entry);

        this.loadIfMissing(ticket);
    }
    public void addQuery(String ticket, QueryConfig.QueryEntry entry) {
        config.queries.put(ticket, entry);
    }

    public void addQueryToMemory(String ticket, String sql, boolean cache, int ttlMinutes) {
        if (config.queries == null)
            config.queries = new HashMap<>();
        QueryConfig.QueryEntry entry = new QueryConfig.QueryEntry();
        entry.sql = sql;
        entry.cache = cache;
        entry.ttlMinutes = ttlMinutes;
        config.queries.put(ticket, entry);
    }

}

