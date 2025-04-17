package ws.prodigy;

public class JdbcUrlBuilder {

    public static String build(String dbType, String host, String port, String dbName) {
        dbType = dbType.toLowerCase();
        return switch (dbType) {
            case "postgresql" -> "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
            case "mysql" -> "jdbc:mysql://" + host + ":" + port + "/" + dbName;
            case "oracle" -> "jdbc:oracle:thin:@" + host + ":" + port + ":" + dbName;
            default -> throw new IllegalArgumentException("Desteklenmeyen veritabanı türü: " + dbType);
        };
    }
}