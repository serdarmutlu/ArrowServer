package ws.prodigy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static spark.Spark.*;

public class FlightApiServer {
    public static void start(FlightCacheManager cache, BufferAllocator allocator, QueryConfig config) {
        port(8080);
        staticFiles.location("/static");

        ObjectMapper mapper = new ObjectMapper();

        // Ana sayfayı yönlendir
        get("/", (req, res) -> {
            res.redirect("/flight.html");
            return null;
        });

        // Bütün ticket listesini ver
        get("/tickets", (req, res) -> {
            res.type("application/json");
            return mapper.writeValueAsString(cache.listTickets());
        });

        // TTL bilgisi ver
        get("/cache-info", (req, res) -> {
            Map<String, Map<String, Object>> result = new HashMap<>();
            for (String ticket : cache.listTickets()) {
                FlightCacheManager.CacheEntry entry = cache.getEntry(ticket);
                if (entry != null) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("loadedAt", entry.loadedAtMillis());
                    info.put("expiresIn", cache.getTTL(ticket));
                    result.put(ticket, info);
                }
            }
            res.type("application/json");
            return mapper.writeValueAsString(result);
        });

        // Veriyi getir (pagination + toplam kayıt sayısı)
        get("/data", (req, res) -> {
            String ticket = req.queryParams("ticket");
            int page = Integer.parseInt(req.queryParams("page"));
            int size = Integer.parseInt(req.queryParams("size"));
            String filter = req.queryParams("filter");

            System.out.println("\n📩 /data çağrıldı, ticket = " + ticket);
            System.out.println("🎯 config'te bu ticket var mı? " + config.queries.containsKey(ticket));

            cache.loadIfMissing(ticket);
            VectorSchemaRoot root = cache.get(ticket);

            List<Map<String, Object>> rows = new ArrayList<>();
            if (root == null || root.getRowCount() == 0) {
                System.out.println("⚠️ root boş veya sıfır kayıt içeriyor");
                res.type("application/json");
                return mapper.writeValueAsString(Map.of(
                        "rows", rows,
                        "total", 0
                ));
            }

            int total = 0;
            for (int i = 0; i < root.getRowCount(); i++) {
                boolean include = true;
                if (filter != null && !filter.isBlank()) {
                    include = false;
                    for (var v : root.getFieldVectors()) {
                        Object val = v.getObject(i);
                        if (val != null && val.toString().toLowerCase().contains(filter.toLowerCase())) {
                            include = true;
                            break;
                        }
                    }
                }
                if (include) total++;
            }

            int start = page * size;
            int end = Math.min(start + size, root.getRowCount());

            int added = 0;
            for (int i = 0; i < root.getRowCount(); i++) {
                boolean include = true;
                if (filter != null && !filter.isBlank()) {
                    include = false;
                    for (var v : root.getFieldVectors()) {
                        Object val = v.getObject(i);
                        if (val != null && val.toString().toLowerCase().contains(filter.toLowerCase())) {
                            include = true;
                            break;
                        }
                    }
                }

                if (include) {
                    if (added >= start && added < start + size) {
                        Map<String, Object> row = new HashMap<>();
                        for (var v : root.getFieldVectors()) {
                            row.put(v.getName(), v.getObject(i));
                        }
                        rows.add(row);
                    }
                    added++;
                }
            }

            res.type("application/json");
            return mapper.writeValueAsString(Map.of(
                    "rows", rows,
                    "total", total
            ));
        });

        // AddTicketEndpoint entegrasyonu
        AddTicketEndpoint.register(cache, new MetadataRepository("metadata.db"));
    }
}
