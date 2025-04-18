package ws.prodigy;

import java.util.Map;

public class GroupedTable {
    private final ArrowTable table;
    private final String groupColumn;

    public GroupedTable(ArrowTable table, String groupColumn) {
        this.table = table;
        this.groupColumn = groupColumn;
    }

    public ArrowTable aggregate(Map<String, String> aggregations) {
        return table.groupBy(groupColumn, aggregations);  // önceki metodu çağırıyor
    }
}
