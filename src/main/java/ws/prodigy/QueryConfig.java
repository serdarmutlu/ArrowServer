package ws.prodigy;

import java.util.Map;

public class QueryConfig {
    public Map<String, QueryEntry> queries;

    public static class QueryEntry {
        public String sql;
        public int ttlMinutes;

        public String dbType;
        public String host;
        public String port;
        public String dbName;

        public String user;
        public String password;

        public String db; // jdbc url
        public boolean initialCache;
        public QueryEntry() {}

    }

}
