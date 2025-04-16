package ws.prodigy;
import static spark.Spark.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.FieldVector;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.arrow.memory.BufferAllocator;


public class FlightApiServer {

    public static void start(FlightCacheManager cache, BufferAllocator allocator, QueryConfig config) {
        port(8080);

        // JSON parser
        ObjectMapper mapper = new ObjectMapper();

        // Konfigürasyonu oku ve cache manager başlat

        get("/", (req, res) -> {
            res.type("text/html");
            try (InputStream in = FlightApiServer.class.getClassLoader().getResourceAsStream("static/flight.html")) {
                if (in == null) return 404;
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        });


        // Bütün ticket listesini ver
        get("/tickets", (req, res) -> {
            res.type("application/json");
            return mapper.writeValueAsString(cache.listTickets());
        });

        // Belirli bir ticket için sayfa sayfa veri getir
        get("/data", (req, res) -> {
            String ticket = req.queryParams("ticket");
            if (ticket == null || ticket.isBlank()) {
                res.status(400);
                return "{\"error\":\"ticket parametresi zorunludur\"}";
            }

            int page = Integer.parseInt(req.queryParams("page") != null ? req.queryParams("page") : "0");
            int size = Integer.parseInt(req.queryParams("size") != null ? req.queryParams("size") : "100");

            try {
                cache.loadIfMissing(ticket); // TTL kontrolü dahil
                VectorSchemaRoot root = cache.get(ticket);

                int total = root.getRowCount();
                int start = page * size;
                int end = Math.min(start + size, total);

                if (start >= total) {
                    res.status(416); // Range Not Satisfiable
                    return "{\"error\":\"istenen sayfa aralığı geçersiz\"}";
                }

                List<Map<String, Object>> rows = new ArrayList<>();

                for (int i = start; i < end; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (FieldVector vec : root.getFieldVectors()) {
                        row.put(vec.getName(), vec.getObject(i));
                    }
                    rows.add(row);
                }

                res.type("application/json");
                return mapper.writeValueAsString(Map.of(
                        "ticket", ticket,
                        "page", page,
                        "size", size,
                        "total", total,
                        "rows", rows
                ));

            } catch (Exception e) {
                res.status(500);
                return mapper.writeValueAsString(Map.of("error", e.getMessage()));
            }
        });

        get("/cache-info", (req, res) -> {
            res.type("application/json");
            Map<String, Map<String, Object>> result = new HashMap<>();

            long now = System.currentTimeMillis();

            for (String ticket : cache.listTickets()) {
                long loadedAt = cache.getLoadedAt(ticket);
                long expiresIn = cache.getExpiresIn(ticket);

                if (loadedAt > 0) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("loadedAt", loadedAt);
                    data.put("expiresIn", expiresIn);
                    result.put(ticket, data);
                }
            }

            return mapper.writeValueAsString(result);
        });


        System.out.println("🌐 Flight API sunucusu http://localhost:8080 adresinde hazır!");
    }
}

