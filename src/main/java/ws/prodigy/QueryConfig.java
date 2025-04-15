package ws.prodigy;

import java.util.Map;

public class QueryConfig {
    public DatabaseConfig database;
    public Map<String, String> queries;

    public static class DatabaseConfig {
        public String host;
        public int port;
        public String dbName;
        public String user;
        public String password;
    }
}