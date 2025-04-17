package ws.prodigy;

import java.util.Map;

public class QueryConfig {
    public DatabaseConfig database;
    public Map<String, QueryEntry> queries;

    public static class DatabaseConfig {
        public String host;
        public int port;
        public String dbName;
        public String user;
        public String password;
    }

    public static class QueryEntry {
        public String sql;
        public boolean cache;
        public int ttlMinutes;
        public String db;
        public String user;
        public String password;
    }
}
