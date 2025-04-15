package ws.prodigy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;

public class QueryConfigLoader {

    public static QueryConfig load(String resourceName) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream in = QueryConfigLoader.class.getClassLoader().getResourceAsStream(resourceName);
            if (in == null) {
                throw new RuntimeException("❌ Resource bulunamadı: " + resourceName);
            }
            return mapper.readValue(in, QueryConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("❌ JSON konfigürasyonu okunamadı: " + resourceName, e);
        }
    }
}

