package ws.prodigy;

import org.apache.arrow.vector.VectorSchemaRoot;

public class ArrowConverter {
    public static ArrowTable fromRoot(VectorSchemaRoot root) {
        ArrowTable table = new ArrowTable();

        // Sütun yapısını kopyala
        root.getSchema().getFields().forEach(field -> table.addColumn(field));

        int rowCount = root.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            int finalI = i;
            Object[] row = root.getFieldVectors().stream()
                    .map(v -> v.getObject(finalI))
                    .toArray();
            table.appendRow(row);
        }

        return table;
    }
}
