package ws.prodigy;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlightCacheManager {

    private final BufferAllocator allocator;
    public final QueryConfig config;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public FlightCacheManager(BufferAllocator allocator, QueryConfig config) {
        this.allocator = allocator;
        this.config = config;
    }

    public boolean contains(String ticket) {
        return get(ticket) != null;
    }

    public record CacheEntry(VectorSchemaRoot root, long loadedAtMillis, long ttlMillis) {
        public boolean isExpired() {
            return System.currentTimeMillis() - loadedAtMillis > ttlMillis;
        }
    }

    public void loadIfMissing(String ticket) {
        if (!cache.containsKey(ticket)) {
            QueryConfig.QueryEntry entry = config.queries.get(ticket);
            if (entry == null) throw new RuntimeException("Ticket bulunamadı: " + ticket);

            try {
                System.out.println("🚦 Loading ticket: " + ticket);
                var root = GenericAdbcLoader.load(allocator, entry);
                System.out.println("✅ Ticket yüklendi: " + root.getRowCount() + " kayıt");
                cache.put(ticket, new CacheEntry(root, System.currentTimeMillis(), entry.ttlMinutes * 60_000L));
            } catch (Exception e) {
                System.out.println("❌ Ticket yüklenemedi: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Yenileme hatası: " + ticket, e);
            }
        }
    }

    public void load(String ticket) {
        if (!this.contains(ticket)) {
            this.loadIfMissing(ticket);
        }
    }

    public void unload(String ticket) {
        CacheEntry removed = cache.remove(ticket);
        if (removed != null && removed.root != null) {
            removed.root.clear(); // Bellek temizliği
            System.out.println("🧹 Cache temizlendi: " + ticket);
        } else {
            System.out.println("⚠️ Cache'de bulunamadı: " + ticket);
        }
        System.gc(); // Garbage Collector’ı tetikle
    }

    public VectorSchemaRoot get(String ticket) {
        CacheEntry entry = cache.get(ticket);
        return entry != null ? entry.root : null;
    }

    public long getTTL(String ticket) {
        CacheEntry entry = cache.get(ticket);
        if (entry == null) return -1;
        long passed = System.currentTimeMillis() - entry.loadedAtMillis;
        return Math.max(0, entry.ttlMillis - passed);
    }

    public List<String> listTickets() {
        return config.queries.keySet().stream().sorted().toList();
    }

    public void delete(String ticket) {
        this.unload(ticket);
        cache.remove(ticket);
    }

    public void refresh(String ticket) {
        cache.remove(ticket);
        loadIfMissing(ticket);
    }

    public void addQueryToMemory(String ticket, String sql, boolean cacheEnabled, int ttlMinutes) {
        if (config.queries == null) config.queries = new HashMap<>();
        QueryConfig.QueryEntry entry = new QueryConfig.QueryEntry();
        entry.sql = sql;
        entry.ttlMinutes = ttlMinutes;
        config.queries.put(ticket, entry);
    }

    public long getTotalBufferBytes() {
        long total = 0;
        for (CacheEntry entry : cache.values()) {
            if (entry.root != null) {
                total += entry.root.getFieldVectors().stream()
                        .mapToLong(vec -> vec.getBufferSize())
                        .sum();
            }
        }
        return total;
    }

    public CacheEntry getEntry(String ticket) {
        return cache.get(ticket);
    }
}