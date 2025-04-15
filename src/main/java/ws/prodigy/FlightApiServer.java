package ws.prodigy;
import static spark.Spark.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.FieldVector;

import java.util.*;

import org.apache.arrow.memory.BufferAllocator;


public class FlightApiServer {

    public static void start(FlightCacheManager cache, BufferAllocator allocator) {
        port(8080);

        // JSON parser
        ObjectMapper mapper = new ObjectMapper();

        // Konfigürasyonu oku ve cache manager başlat


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

        System.out.println("🌐 Flight API sunucusu http://localhost:8080 adresinde hazır!");
    }
}
