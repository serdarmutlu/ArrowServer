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

    public void insertQuery(String ticket, QueryConfig.QueryEntry entry) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            String sql = "INSERT INTO queries (ticket, sql, db_type, host, port, db_name, user, password, cache, ttl_minutes, initial_cache) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ticket);
                stmt.setString(2, entry.sql);
                stmt.setString(3, entry.dbType);
                stmt.setString(4, entry.host);
                stmt.setString(5, entry.port);
                stmt.setString(6, entry.dbName);
                stmt.setString(7, entry.user);
                stmt.setString(8, entry.password);
                stmt.setBoolean(9, entry.cache);
                stmt.setInt(10, entry.ttlMinutes);
                stmt.setBoolean(11, entry.initialCache);
                stmt.executeUpdate();
            }
        }
    }

    public void updateQuery(String ticket, QueryConfig.QueryEntry entry) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            String sql = "UPDATE queries SET sql=?, db_type=?, host=?, port=?, db_name=?, user=?, password=?, cache=?, ttl_minutes=?, initial_cache=? " +
                    "WHERE ticket=?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, entry.sql);
                stmt.setString(2, entry.dbType);
                stmt.setString(3, entry.host);
                stmt.setString(4, entry.port);
                stmt.setString(5, entry.dbName);
                stmt.setString(6, entry.user);
                stmt.setString(7, entry.password);
                stmt.setBoolean(8, entry.cache);
                stmt.setInt(9, entry.ttlMinutes);
                stmt.setBoolean(10, entry.initialCache);
                stmt.setString(11, ticket);
                stmt.executeUpdate();
            }
        }
    }


//    public void save(String ticket, QueryConfig.QueryEntry entry) throws SQLException {
//        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
//            String sql = "INSERT INTO queries (ticket, sql, db_type, host, port, db_name, user, password, cache, ttl_minutes, initial_cache) " +
//                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
//                    "ON CONFLICT(ticket) DO UPDATE SET sql=?, db_type=?, host=?, port=?, db_name=?, user=?, password=?, cache=?, ttl_minutes=?, initial_cache=?";
//
//            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
//                stmt.setString(1, ticket);
//                stmt.setString(2, entry.sql);
//                stmt.setString(3, entry.dbType);
//                stmt.setString(4, entry.host);
//                stmt.setString(5, entry.port);
//                stmt.setString(6, entry.dbName);
//                stmt.setString(7, entry.user);
//                stmt.setString(8, entry.password);
//                stmt.setBoolean(9, entry.cache);
//                stmt.setInt(10, entry.ttlMinutes);
//                stmt.setBoolean(11, entry.initialCache);
//
//                stmt.executeUpdate();
//            }
//        }
//    }

//    public void load(QueryConfig config) throws SQLException {
//        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
//             Statement stmt = conn.createStatement();
//             ResultSet rs = stmt.executeQuery("SELECT * FROM queries")) {
//
//            while (rs.next()) {
//                QueryConfig.QueryEntry entry = new QueryConfig.QueryEntry();
//                entry.sql = rs.getString("sql");
//                entry.dbType = rs.getString("db_type");
//                entry.host = rs.getString("host");
//                entry.port = rs.getString("port");
//                entry.dbName = rs.getString("db_name");
//                entry.user = rs.getString("user");
//                entry.password = rs.getString("password");
//                entry.cache = rs.getBoolean("cache");
//                entry.ttlMinutes = rs.getInt("ttl_minutes");
//                entry.initialCache = rs.getBoolean("initial_cache");
//                String ticket = rs.getString("ticket");
//                config.queries.put(ticket, entry);
//            }
//        }
//    }

    public List<TableMetadata> loadAll() throws SQLException {
        List<TableMetadata> result = new ArrayList<>();

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT ticket, sql, db_type, host, port, db_name, user, password, cache, ttl_minutes, initial_cache FROM queries")) {

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
                        rs.getString("password"),
                        rs.getBoolean("initial_cache")
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
            boolean cache,
            long ttlMillis,
            String user,
            String password,
            boolean initial_cache
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
                    ttl_minutes INTEGER,
                    initial_cache BOOLEAN
                )
            """);
        }
    }
}

