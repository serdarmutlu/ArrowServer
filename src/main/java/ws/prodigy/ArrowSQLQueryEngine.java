package ws.prodigy;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;

public class ArrowSQLQueryEngine {

    public static ArrowTable execute(ArrowTable table, String sql) {
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof Select)) {
                throw new IllegalArgumentException("Only SELECT statements are supported.");
            }

            PlainSelect selectBody = (PlainSelect) ((Select) stmt).getSelectBody();
            ArrowTable filtered = table;

            // WHERE
            Expression whereExpr = selectBody.getWhere();
            if (whereExpr != null) {
                filtered = filtered.filter(whereExpr);
            }

            // GROUP BY
            GroupByElement groupBy = selectBody.getGroupBy();
            ArrowTable result;

            if (groupBy != null) {
                List<Expression> groupExprs = groupBy.getGroupByExpressions();
                if (groupExprs.size() != 1) {
                    throw new UnsupportedOperationException("Only one GROUP BY column is supported for now.");
                }

                String groupCol = ((Column) groupExprs.get(0)).getColumnName();

                Map<String, String> aggregations = new LinkedHashMap<>();
                List<String> projectionCols = new ArrayList<>();

                for (SelectItem item : selectBody.getSelectItems()) {
                    if (item instanceof SelectExpressionItem se) {
                        Expression expr = se.getExpression();
                        if (expr instanceof Function func) {
                            String funcName = func.getName().toLowerCase();
                            String colName = ((Column) func.getParameters().getExpressions().get(0)).getColumnName();
                            aggregations.put(colName, funcName);
                        } else if (expr instanceof Column col) {
                            projectionCols.add(col.getColumnName());
                        }
                    }
                }

                result = filtered.groupBy(groupCol, aggregations);

                if (!projectionCols.isEmpty()) {
                    for (var entry : aggregations.entrySet()) {
                        projectionCols.add(entry.getValue() + "_" + entry.getKey());
                    }
                    result = result.select(projectionCols.toArray(new String[0]));
                }

                if (selectBody.getHaving() != null) {
                    result = result.filter(selectBody.getHaving());
                }
            } else {
                // Sadece projection varsa
                List<String> projection = new ArrayList<>();
                for (SelectItem item : selectBody.getSelectItems()) {
                    if (item instanceof SelectExpressionItem se && se.getExpression() instanceof Column col) {
                        projection.add(col.getColumnName());
                    }
                }
                result = projection.isEmpty() ? filtered : filtered.select(projection.toArray(new String[0]));
            }

            // ORDER BY
            List<OrderByElement> orderByElements = selectBody.getOrderByElements();
            if (orderByElements != null && !orderByElements.isEmpty()) {
                OrderByElement order = orderByElements.get(0);
                Expression orderExpr = order.getExpression();
                String orderCol = (orderExpr instanceof Function func)
                        ? func.getName().toLowerCase() + "_" +
                        ((Column) func.getParameters().getExpressions().get(0)).getColumnName()
                        : ((Column) orderExpr).getColumnName();

                boolean ascending = order.isAsc();
                result = result.sortBy(orderCol, ascending);
            }

            // DISTINCT
            if (selectBody.getDistinct() != null) {
                result = result.distinct();
            }

            // LIMIT
            Limit limit = selectBody.getLimit();
            if (limit != null && limit.getRowCount() instanceof LongValue lv) {
                result = result.limitRows((int) lv.getValue());
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("SQL execution failed: " + e.getMessage(), e);
        }
    }
    public static ArrowTable execute(ArrowTable table, String sql, Map<String, ArrowTable> externalTables) {
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof Select)) {
                throw new IllegalArgumentException("Only SELECT statements are supported.");
            }
            PlainSelect selectBody = (PlainSelect) ((Select) stmt).getSelectBody();

            // Örnek: externalTables kullanılabilir hale getirilebilir (örnek JOIN veya SUBQUERY desteği için)
            // Şimdilik sadece table ile devam:
            return execute(table, sql); // default davranışı çağır

        } catch (Exception e) {
            throw new RuntimeException("SQL execution failed: " + e.getMessage(), e);
        }
    }


}
