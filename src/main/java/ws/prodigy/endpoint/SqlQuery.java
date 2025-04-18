package ws.prodigy.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import ws.prodigy.FlightCacheManager;
import ws.prodigy.ArrowTable;
import ws.prodigy.ArrowConverter;

import java.util.Map;

import static spark.Spark.post;

public class SqlQuery {
    public static void register(FlightCacheManager cache) {
        post("/sql-query", (req, res) -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
                Map<String, Object> body = mapper.readValue(req.body(), Map.class);
                String sql = (String) body.get("sql");

                System.out.println("\n🧪 SQL Query alındı: " + sql);

                Statement statement = CCJSqlParserUtil.parse(sql);
                if (!(statement instanceof Select)) {
                    res.status(400);
                    return "Yalnızca SELECT sorguları destekleniyor";
                }

                Select select = (Select) statement;
                PlainSelect plain = (PlainSelect) select.getSelectBody();
                String ticket = plain.getFromItem().toString();

                var root = cache.get(ticket);
                if (root == null) {
                    res.status(404);
                    return "Cache'te bu ticket bulunamadı: " + ticket;
                }

                ArrowTable table = ArrowConverter.fromRoot(root);
                ArrowTable resultTable = table.query(sql);

                res.type("application/json");
                return mapper.writeValueAsString(resultTable.toMapList());
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                res.type("text/plain");
                return "Hata: " + e.getClass().getSimpleName() + " - " + e.getMessage();
            }
        });
    }
}
