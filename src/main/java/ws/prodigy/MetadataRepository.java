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

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    public void save(String ticket, QueryConfig.QueryEntry entry, String user, String password) throws SQLException {
        try (Connection conn = connect()) {
            String sql = "INSERT INTO queries (ticket, sql, db_type, host, port, db_name, user, password, cache, ttl_minutes) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT(ticket) DO UPDATE SET sql=?, db_type=?, host=?, port=?, db_name=?, user=?, password=?, cache=?, ttl_minutes=?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ticket);
                stmt.setString(2, entry.sql);
                stmt.setString(3, entry.dbType);
                stmt.setString(4, entry.host);
                stmt.setString(5, entry.port);
                stmt.setString(6, entry.dbName);
                stmt.setString(7, user);
                stmt.setString(8, password);
                stmt.setBoolean(9, entry.cache);
                stmt.setInt(10, entry.ttlMinutes);

                // update kısmı
                stmt.setString(11, entry.sql);
                stmt.setString(12, entry.dbType);
                stmt.setString(13, entry.host);
                stmt.setString(14, entry.port);
                stmt.setString(15, entry.dbName);
                stmt.setString(16, user);
                stmt.setString(17, password);
                stmt.setBoolean(18, entry.cache);
                stmt.setInt(19, entry.ttlMinutes);

                stmt.executeUpdate();
            }
        }
    }

    public List<TableMetadata> loadAll() throws SQLException {
        List<TableMetadata> result = new ArrayList<>();

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT ticket, sql, db_type, host, port, db_name, user, password, cache, ttl_minutes FROM queries")) {

            while (rs.next()) {
                result.add(new TableMetadata(
                        rs.getString("ticket"),
                        rs.getString("sql"),
                        rs.getString("db_type"),
                        rs.getString("host"),
                        rs.getString("port"),
                        rs.getString("db_name"),
                        rs.getBoolean("cache"),
                        rs.getInt("ttl_minutes") * 60_000L,
                        rs.getString("user"),
                        rs.getString("password")
                ));
            }
        }

        return result;
    }

    public record TableMetadata(
            String ticket,
            String sql,
            String dbType,
            String host,
            String port,
            String dbName,
            boolean autoCache,
            long ttlMillis,
            String user,
            String password
    ) {}

    public void init() throws SQLException {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS queries (
                    ticket TEXT PRIMARY KEY,
                    sql TEXT NOT NULL,
                    db_type TEXT,
                    host TEXT,
                    port TEXT,
                    db_name TEXT,
                    user TEXT,
                    password TEXT,
                    cache BOOLEAN,
                    ttl_minutes INTEGER
                )
            """);
        }
    }
}

