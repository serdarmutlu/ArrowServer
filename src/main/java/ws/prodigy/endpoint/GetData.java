package ws.prodigy.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.arrow.vector.VectorSchemaRoot;
import spark.Request;
import spark.Response;
import ws.prodigy.FlightCacheManager;
import ws.prodigy.QueryConfig;

import java.util.*;

import static spark.Spark.get;

public class GetData {
    public static void register(FlightCacheManager cache, QueryConfig config) {

        get("/data", (Request req, Response res) -> {
            ObjectMapper mapper = new ObjectMapper();

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
    }
}