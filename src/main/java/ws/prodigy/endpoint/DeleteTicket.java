package ws.prodigy.endpoint;

import ws.prodigy.QueryConfig;
import ws.prodigy.FlightCacheManager;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Connection;

import static spark.Spark.delete;

public class DeleteTicket {
    public static void register(FlightCacheManager cache, QueryConfig config) {
        delete("/delete-ticket", (req, res) -> {
            String ticket = req.queryParams("ticket");

            config.queries.remove(ticket);
            cache.refresh(ticket); // varsa cache'ten çıkar

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:metadata.db");
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM queries WHERE ticket = ?")) {
                stmt.setString(1, ticket);
                stmt.executeUpdate();
            }

            res.status(200);
            return "OK";
        });
    }
}