package ws.prodigy;

import org.apache.arrow.vector.VectorSchemaRoot;

import java.util.HashMap;
import java.util.Map;

public class ColumnarStore {

    private final Map<String, VectorSchemaRoot> store = new HashMap<>();

    public boolean has(String ticket) {
        return store.containsKey(ticket);
    }

    public VectorSchemaRoot get(String ticket) {
        return store.get(ticket);
    }

    public void save(String ticket, VectorSchemaRoot root) {
        store.put(ticket, root);
    }

}
