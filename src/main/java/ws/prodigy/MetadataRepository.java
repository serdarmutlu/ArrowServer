package ws.prodigy;

// MetadataRepository.java - Embedded DB ile metadata yöneticisi

import java.sql.*;

import java.util.ArrayList;
import java.util.List;

public class MetadataRepository {
    private final String dbPath;

    public MetadataRepository(String dbPath) {
        this.dbPath = dbPath;
    }

    public void init() throws SQLException {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS queries (
              ticket TEXT PRIMARY KEY,
              sql TEXT NOT NULL,
              db TEXT,
              user TEXT,
              password TEXT,
              cache BOOLEAN,
              ttl_minutes INTEGER
            )
        """);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    public void save(String ticket, QueryConfig.QueryEntry entry, String db, String user, String password) throws SQLException {
        try (Connection conn = connect()) {
            String sql = "INSERT INTO queries (ticket, sql, db, user, password, cache, ttl_minutes) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(ticket) DO UPDATE SET sql=?, db=?, user=?, password=?, cache=?, ttl_minutes=?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ticket);
                stmt.setString(2, entry.sql);
                stmt.setString(3, db);
                stmt.setString(4, user);
                stmt.setString(5, password);
                stmt.setBoolean(6, entry.cache);
                stmt.setInt(7, entry.ttlMinutes);

                // update kısmı
                stmt.setString(8, entry.sql);
                stmt.setString(9, db);
                stmt.setString(10, user);
                stmt.setString(11, password);
                stmt.setBoolean(12, entry.cache);
                stmt.setInt(13, entry.ttlMinutes);

                stmt.executeUpdate();
            }
        }
    }

    public List<TableMetadata> loadAll() throws SQLException {
        List<TableMetadata> result = new ArrayList<>();

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT ticket, sql, db, user, password, cache, ttl_minutes FROM queries")) {

            while (rs.next()) {
                result.add(new TableMetadata(
                        rs.getString("ticket"),
                        rs.getString("sql"),
                        rs.getBoolean("cache"),
                        rs.getInt("ttl_minutes") * 60_000L,
                        rs.getString("db"),
                        rs.getString("user"),
                        rs.getString("password")
                ));
            }
        }

        return result;
    }

    public record TableMetadata(String ticket, String sql, boolean autoCache, long ttlMillis, String db, String user, String password) {}
}
