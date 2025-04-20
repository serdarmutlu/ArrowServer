package ws.prodigy.endpoint;

import ws.prodigy.QueryConfig;
import ws.prodigy.FlightCacheManager;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLException;

import static spark.Spark.delete;

public class DeleteTicket {
    public static void register(FlightCacheManager cache, QueryConfig config) {
        delete("/ticket/delete/:ticketid", (req, res) -> {
            String ticket = req.params(":ticketid");

            config.queries.remove(ticket);
            cache.refresh(ticket); // varsa cache'ten çıkar

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:metadata.db");
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM queries WHERE ticket = ?")) {
                stmt.setString(1, ticket);
                stmt.executeUpdate();
            } catch(Exception e) {
                System.out.println("Not able to delete");
            }

            res.status(200);
            return "OK";
        });
    }
}