package ws.prodigy;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.schema.Column;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.Predicate;

public class ArrowTable {
    private final BufferAllocator allocator;
    private final List<FieldVector> vectors = new ArrayList<>();
    private final List<Field> fields = new ArrayList<>();
    private final Map<String, FieldVector> columnMap = new HashMap<>();

    // Varsayılan: sınırsız limit
    public ArrowTable() {
        this(Long.MAX_VALUE);
    }

    // İsteğe bağlı bellek limiti
    public ArrowTable(long maxMemoryLimitBytes) {
        this.allocator = new RootAllocator(maxMemoryLimitBytes);
    }

    public void addColumn(Field field) {
        ArrowType type = field.getType();
        String name = field.getName();

        if (type instanceof ArrowType.Int) {
            this.addIntColumn(name);
        } else if (type instanceof ArrowType.FloatingPoint) {
            this.addFloatColumn(name);
        } else if (type instanceof ArrowType.Utf8) {
            this.addStringColumn(name);
        } else if (type instanceof ArrowType.Date) {
            this.addDateColumn(name);
        } else {
            throw new UnsupportedOperationException("Unsupported column type: " + type);
        }
    }

    public void addIntColumn(String name) {
        Field field = new Field(name, FieldType.nullable(new ArrowType.Int(32, true)), null);
        IntVector vector = new IntVector(name, allocator);
        vector.allocateNew();
        fields.add(field);
        vectors.add(vector);
        columnMap.put(name, vector);
    }

    public void addStringColumn(String name) {
        Field field = new Field(name, FieldType.nullable(new ArrowType.Utf8()), null);
        VarCharVector vector = new VarCharVector(name, allocator);
        vector.allocateNew();
        fields.add(field);
        vectors.add(vector);
        columnMap.put(name, vector);
    }

    public void addFloatColumn(String name) {
        Field field = new Field(name, FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE)), null);
        Float4Vector vector = new Float4Vector(name, allocator);
        vector.allocateNew();
        fields.add(field);
        vectors.add(vector);
        columnMap.put(name, vector);
    }
    public void addDateColumn(String name) {
        Field field = new Field(name, FieldType.nullable(new ArrowType.Date(DateUnit.DAY)), null);
        DateDayVector vector = new DateDayVector(name, allocator);
        vector.allocateNew();
        fields.add(field);
        vectors.add(vector);
        columnMap.put(name, vector);
    }

    public int getRowCount() {
        if (vectors.isEmpty()) return 0;
        return vectors.get(0).getValueCount();
    }

    public Object getValue(String columnName, int rowIndex) {
        FieldVector vec = columnMap.get(columnName);
        if (vec == null) throw new IllegalArgumentException("Column not found: " + columnName);
        return vec.getObject(rowIndex);
    }

    public List<Object> getRow(int rowIndex) {
        List<Object> values = new ArrayList<>();
        for (FieldVector vec : vectors) {
            values.add(vec.getObject(rowIndex));
        }
        return values;
    }


    public void appendRow(Object[] values) {
        for (int i = 0; i < values.length; i++) {
            FieldVector vector = vectors.get(i);
            Object value = values[i];

            if (vector instanceof IntVector) {
                ((IntVector) vector).setSafe(vector.getValueCount(), value == null ? 0 : ((Number) value).intValue());
            } else if (vector instanceof Float4Vector) {
                ((Float4Vector) vector).setSafe(vector.getValueCount(), value == null ? 0 : ((Number) value).floatValue());
            } else if (vector instanceof VarCharVector) {
                if (value == null) {
                    ((VarCharVector) vector).setNull(vector.getValueCount());
                } else {
                    ((VarCharVector) vector).setSafe(vector.getValueCount(), value.toString().getBytes());
                }
            } else if (vector instanceof DateDayVector) {
                if (value == null) {
                    ((DateDayVector) vector).setNull(vector.getValueCount());
                } else if (value instanceof Number n) {
                    ((DateDayVector) vector).setSafe(vector.getValueCount(), n.intValue());
                } else if (value instanceof java.util.Date d) {
                    int days = (int) (d.getTime() / (1000 * 60 * 60 * 24));
                    ((DateDayVector) vector).setSafe(vector.getValueCount(), days);
                } else {
                    throw new IllegalArgumentException("Unsupported date value: " + value);
                }
            } else {
                throw new IllegalArgumentException("Unsupported vector type at column " + i);
            }
            vector.setValueCount(vector.getValueCount() + 1);
        }
    }


    public void print() {
        int rowCount = vectors.isEmpty() ? 0 : vectors.get(0).getValueCount();
        for (int i = 0; i < rowCount; i++) {
            for (FieldVector vector : vectors) {
                Object val = vector.getObject(i);
                System.out.print(val + "\t");
            }
            System.out.println();
        }
    }

    public void close() {
        vectors.forEach(FieldVector::close);
        allocator.close();
    }

    public ArrowTable filter(String columnName, Predicate<Object> predicate) {
        FieldVector targetVector = columnMap.get(columnName);
        if (targetVector == null) {
            throw new IllegalArgumentException("Column not found: " + columnName);
        }

        ArrowTable result = new ArrowTable();

        // Aynı kolonları oluştur
        for (Field field : fields) {
            ArrowType type = field.getType();
            if (type instanceof ArrowType.Int) {
                result.addIntColumn(field.getName());
            }
            else if (type instanceof ArrowType.Utf8) {
                result.addStringColumn(field.getName());
            }
            else if (type instanceof ArrowType.FloatingPoint) {
                result.addFloatColumn(field.getName());
            }
            // Diğer veri tipleri eklenecek
        }

        int rowCount = targetVector.getValueCount();

        for (int i = 0; i < rowCount; i++) {
            Object value = targetVector.getObject(i);
            if (predicate.test(value)) {
                // Uygun satırı yeni tabloya kopyala
                Object[] row = new Object[vectors.size()];
                for (int col = 0; col < vectors.size(); col++) {
                    row[col] = vectors.get(col).getObject(i);
                }
                result.appendRow(row);
            }
        }

        return result;
    }

    public ArrowTable filter(Expression expr) {
        if (!(expr instanceof BinaryExpression be)) {
            throw new UnsupportedOperationException("Only simple binary expressions are supported.");
        }

        Expression leftExpr = be.getLeftExpression();
        String col;

        if (leftExpr instanceof Column c) {
            col = c.getColumnName();
        } else if (leftExpr instanceof Function f) {
            String func = f.getName().toLowerCase();
            String arg = ((Column) f.getParameters().getExpressions().get(0)).getColumnName();
            col = func + "_" + arg;  // örn. sum_salary
        } else {
            throw new UnsupportedOperationException("Unsupported left-hand expression: " + leftExpr);
        }

        String op = be.getStringExpression();
        Expression rightExpr = be.getRightExpression();

        FieldVector vec = columnMap.get(col);
        if (vec == null) {
            throw new IllegalArgumentException("Column not found: " + col);
        }

        Comparable<?> right = switch (rightExpr) {
            case LongValue lv -> vec instanceof Float4Vector ? (float) lv.getValue() : lv.getValue();
            case DoubleValue dv -> vec instanceof IntVector ? (int) dv.getValue() : dv.getValue();
            case StringValue sv -> sv.getValue();
            default -> throw new UnsupportedOperationException("Unsupported right-hand value type.");
        };

        return this.filter(col, v -> {
            Comparable left = (Comparable) v;
            return switch (op) {
                case "=" -> left.compareTo(right) == 0;
                case ">" -> left.compareTo(right) > 0;
                case "<" -> left.compareTo(right) < 0;
                case ">=" -> left.compareTo(right) >= 0;
                case "<=" -> left.compareTo(right) <= 0;
                case "!=" -> left.compareTo(right) != 0;
                default -> throw new UnsupportedOperationException("Unsupported operator: " + op);
            };
        });
    }




    public ArrowTable select(String... columnNames) {
        ArrowTable result = new ArrowTable();

        // 1. Kolonları oluştur
        for (String colName : columnNames) {
            FieldVector sourceVector = columnMap.get(colName);
            if (sourceVector == null) {
                throw new IllegalArgumentException("Column not found: " + colName);
            }

            Field field = sourceVector.getField();
            ArrowType type = field.getType();

            if (type instanceof ArrowType.Int) {
                result.addIntColumn(colName);
            }
            else if (type instanceof ArrowType.Utf8) {
                result.addStringColumn(colName);
            }
            else if (type instanceof ArrowType.FloatingPoint) {
                result.addFloatColumn(colName);
            }
            // Buraya diğer tipler eklenecek (String, Float, vb.)
        }

        // 2. Satırları kopyala
        int rowCount = vectors.get(0).getValueCount();
        for (int i = 0; i < rowCount; i++) {
            Object[] row = new Object[columnNames.length];
            for (int j = 0; j < columnNames.length; j++) {
                row[j] = columnMap.get(columnNames[j]).getObject(i);
            }
            result.appendRow(row);
        }

        return result;
    }

    public ArrowTable groupBy(String groupColumn, String valueColumn, String function) {
        FieldVector groupVec = columnMap.get(groupColumn);
        FieldVector valueVec = columnMap.get(valueColumn);

        if (groupVec == null || valueVec == null) {
            throw new IllegalArgumentException("Group or value column not found.");
        }

        // Veriyi şu yapıda topla: {grupDeğeri -> List<value>}
        Map<Object, List<Number>> groups = new LinkedHashMap<>();

        int rowCount = groupVec.getValueCount();
        for (int i = 0; i < rowCount; i++) {
            Object key = groupVec.getObject(i);
            Object val = valueVec.getObject(i);
            if (!(val instanceof Number numVal)) continue;

            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(numVal);
        }

        // Yeni tablo
        ArrowTable result = new ArrowTable();

        // Grup kolon tipi
        if (groupVec instanceof IntVector) {
            result.addIntColumn(groupColumn);
        } else {
            result.addStringColumn(groupColumn);
        }

        // Sonuç kolon adı ve tipi
        String resultColName = function.toLowerCase() + "_" + valueColumn;

        boolean isFloat = valueVec instanceof Float4Vector;
        if (function.equalsIgnoreCase("avg") || isFloat) {
            result.addFloatColumn(resultColName);
        } else {
            result.addIntColumn(resultColName);
        }

        // Hesapla
        for (Map.Entry<Object, List<Number>> entry : groups.entrySet()) {
            Object key = entry.getKey();
            List<Number> values = entry.getValue();

            Number resultValue = switch (function.toLowerCase()) {
                case "sum" -> values.stream().mapToDouble(Number::doubleValue).sum();
                case "avg" -> values.stream().mapToDouble(Number::doubleValue).average().orElse(0.0);
                case "min" -> values.stream().mapToDouble(Number::doubleValue).min().orElse(0.0);
                case "max" -> values.stream().mapToDouble(Number::doubleValue).max().orElse(0.0);
                case "count" -> values.size();
                default -> throw new UnsupportedOperationException("Unsupported function: " + function);
            };

            // Float dönüşümünü ayarlayalım
            FieldVector resultVec = result.columnMap.get(function.toLowerCase() + "_" + valueColumn);

            if (resultVec instanceof Float4Vector) {
                float floatVal = resultValue instanceof Double d ? d.floatValue() : resultValue.floatValue();
                result.appendRow(new Object[] { key, floatVal });
            } else if (resultVec instanceof IntVector) {
                result.appendRow(new Object[] { key, resultValue.intValue() });
            } else {
                throw new IllegalStateException("Unsupported result column type: " + resultVec.getClass());
            }
        }

        return result;
    }

    public ArrowTable groupBy(String groupColumn, Map<String, String> aggregations) {
        FieldVector groupVec = columnMap.get(groupColumn);
        if (groupVec == null) {
            throw new IllegalArgumentException("Group column not found: " + groupColumn);
        }

        // Her satır için grubu belirleyip her kolon için değerleri topla
        Map<Object, Map<String, List<Number>>> groupedData = new LinkedHashMap<>();

        int rowCount = groupVec.getValueCount();

        for (int i = 0; i < rowCount; i++) {
            Object key = groupVec.getObject(i);

            groupedData.putIfAbsent(key, new HashMap<>());

            for (Map.Entry<String, String> entry : aggregations.entrySet()) {
                String colName = entry.getKey();
                FieldVector vec = columnMap.get(colName);
                if (!(vec.getObject(i) instanceof Number val)) continue;

                groupedData.get(key)
                        .computeIfAbsent(colName, k -> new ArrayList<>())
                        .add(val);
            }
        }

        // Sonuç tablosu
        ArrowTable result = new ArrowTable();

        // Grup kolonu
        if (groupVec instanceof IntVector) {
            result.addIntColumn(groupColumn);
        } else {
            result.addStringColumn(groupColumn);
        }

        // Sonuç kolonlarını ekle
        Map<String, FieldVector> resultVectors = new HashMap<>();
        for (Map.Entry<String, String> entry : aggregations.entrySet()) {
            String col = entry.getKey();
            String func = entry.getValue();
            String resultColName = func.toLowerCase() + "_" + col;

            FieldVector vec = columnMap.get(col);
            if (func.equalsIgnoreCase("avg") || vec instanceof Float4Vector) {
                result.addFloatColumn(resultColName);
            } else {
                result.addIntColumn(resultColName);
            }
        }

        // Değerleri hesaplayıp satır satır ekle
        for (Map.Entry<Object, Map<String, List<Number>>> group : groupedData.entrySet()) {
            Object key = group.getKey();
            Map<String, List<Number>> data = group.getValue();

            List<Object> row = new ArrayList<>();
            row.add(key);

            for (Map.Entry<String, String> entry : aggregations.entrySet()) {
                String col = entry.getKey();
                String func = entry.getValue().toLowerCase();
                List<Number> values = data.getOrDefault(col, List.of());

                Number val = switch (func) {
                    case "sum" -> values.stream().mapToDouble(Number::doubleValue).sum();
                    case "avg" -> values.stream().mapToDouble(Number::doubleValue).average().orElse(0.0);
                    case "min" -> values.stream().mapToDouble(Number::doubleValue).min().orElse(0.0);
                    case "max" -> values.stream().mapToDouble(Number::doubleValue).max().orElse(0.0);
                    case "count" -> values.size();
                    default -> throw new UnsupportedOperationException("Unknown function: " + func);
                };

                FieldVector resultVec = result.columnMap.get(func + "_" + col);
                if (resultVec instanceof Float4Vector) {
                    row.add(val.floatValue());
                } else {
                    row.add(val.intValue());
                }
            }

            result.appendRow(row.toArray());
        }

        return result;
    }

    public GroupedTable groupBy(String groupColumn) {
        return new GroupedTable(this, groupColumn);
    }

    public void writeToCsv(String filePath) {
        try (Writer writer = new FileWriter(filePath)) {
            writeToCsv(writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CSV", e);
        }
    }

    public void writeToCsv(Writer writer) throws IOException {
        int rowCount = vectors.isEmpty() ? 0 : vectors.get(0).getValueCount();

        // 1. Başlık satırı
        for (int i = 0; i < fields.size(); i++) {
            writer.write(fields.get(i).getName());
            if (i < fields.size() - 1) {
                writer.write(",");
            }
        }
        writer.write("\n");

        // 2. Satırlar
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < vectors.size(); col++) {
                Object val = vectors.get(col).getObject(row);
                if (val != null) {
                    writer.write(escapeCsv(val.toString()));
                }
                if (col < vectors.size() - 1) {
                    writer.write(",");
                }
            }
            writer.write("\n");
        }

        writer.flush();
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public ArrowTable query(String sql) {
        return ArrowSQLQueryEngine.execute(this, sql);
    }

    public ArrowTable query(String sql, Map<String, ArrowTable> externalTables) {
        return ArrowSQLQueryEngine.execute(this, sql, externalTables);
    }


    public ArrowTable sortBy(String columnName, boolean ascending) {
        FieldVector vec = columnMap.get(columnName);
        if (vec == null) {
            throw new IllegalArgumentException("Column not found: " + columnName);
        }

        int rowCount = vec.getValueCount();

        // Satır indekslerini taşıyan liste
        List<Integer> rowIndexes = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) rowIndexes.add(i);

        // Sıralama
        rowIndexes.sort((i1, i2) -> {
            Object raw1 = vec.getObject(i1);
            Object raw2 = vec.getObject(i2);

            Comparable o1 = (raw1 instanceof Comparable) ? (Comparable) raw1 : raw1.toString();
            Comparable o2 = (raw2 instanceof Comparable) ? (Comparable) raw2 : raw2.toString();

            int result = o1.compareTo(o2);
            return ascending ? result : -result;
        });

        // Yeni tabloyu oluştur
        ArrowTable sorted = new ArrowTable();
        for (Field field : fields) {
            ArrowType type = field.getType();
            if (type instanceof ArrowType.Int) sorted.addIntColumn(field.getName());
            else if (type instanceof ArrowType.FloatingPoint) sorted.addFloatColumn(field.getName());
            else if (type instanceof ArrowType.Utf8) sorted.addStringColumn(field.getName());
            else throw new UnsupportedOperationException("Unsupported column type: " + type);
        }

        for (int row : rowIndexes) {
            Object[] values = new Object[vectors.size()];
            for (int col = 0; col < vectors.size(); col++) {
                values[col] = vectors.get(col).getObject(row);
            }
            sorted.appendRow(values);
        }

        return sorted;
    }

    public ArrowTable limitRows(int n) {
        ArrowTable result = new ArrowTable();

        // Kolon yapısını oluştur
        for (Field field : fields) {
            ArrowType type = field.getType();
            if (type instanceof ArrowType.Int) result.addIntColumn(field.getName());
            else if (type instanceof ArrowType.FloatingPoint) result.addFloatColumn(field.getName());
            else if (type instanceof ArrowType.Utf8) result.addStringColumn(field.getName());
            else throw new UnsupportedOperationException("Unsupported column type: " + type);
        }

        int rowCount = Math.min(n, vectors.get(0).getValueCount());
        for (int i = 0; i < rowCount; i++) {
            Object[] values = new Object[vectors.size()];
            for (int j = 0; j < vectors.size(); j++) {
                values[j] = vectors.get(j).getObject(i);
            }
            result.appendRow(values);
        }

        return result;
    }

    public ArrowTable distinct() {
        ArrowTable result = new ArrowTable();

        for (Field field : fields) {
            ArrowType type = field.getType();
            if (type instanceof ArrowType.Int) result.addIntColumn(field.getName());
            else if (type instanceof ArrowType.FloatingPoint) result.addFloatColumn(field.getName());
            else if (type instanceof ArrowType.Utf8) result.addStringColumn(field.getName());
            else throw new UnsupportedOperationException("Unsupported column type: " + type);
        }

        Set<String> seen = new HashSet<>();
        int rowCount = vectors.get(0).getValueCount();

        for (int i = 0; i < rowCount; i++) {
            StringBuilder key = new StringBuilder();
            Object[] row = new Object[vectors.size()];
            for (int j = 0; j < vectors.size(); j++) {
                Object val = vectors.get(j).getObject(i);
                row[j] = val;
                key.append(val).append("|");  // Satırın benzersiz anahtarı
            }

            if (seen.add(key.toString())) {
                result.appendRow(row);
            }
        }
        return result;
    }

    public ArrowTable join(ArrowTable right, String leftKey, String rightKey, boolean isLeftJoin) {
        ArrowTable result = new ArrowTable();

        for (Field field : this.fields) result.addColumn(field);
        for (Field field : right.fields) result.addColumn(field);

        int leftCount = this.getRowCount();
        int rightCount = right.getRowCount();

        for (int i = 0; i < leftCount; i++) {
            Object leftVal = this.getValue(leftKey, i);
            boolean matched = false;

            for (int j = 0; j < rightCount; j++) {
                Object rightVal = right.getValue(rightKey, j);
                if (leftVal != null && leftVal.equals(rightVal)) {
                    List<Object> row = new ArrayList<>();
                    row.addAll(this.getRow(i));
                    row.addAll(right.getRow(j));
                    result.appendRow(row.toArray());
                    matched = true;
                }
            }

            if (isLeftJoin && !matched) {
                List<Object> row = new ArrayList<>();
                row.addAll(this.getRow(i));
                for (int k = 0; k < right.fields.size(); k++) {
                    row.add(null);  // sağ tablonun boş değerleri
                }
                result.appendRow(row.toArray());
            }
        }

        return result;
    }

    public List<Map<String, Object>> toMapList() {
        List<Map<String, Object>> list = new ArrayList<>();
        int rowCount = getRowCount();

        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (FieldVector vec : vectors) {
                row.put(vec.getName(), vec.getObject(i));
            }
            list.add(row);
        }

        return list;
    }

}
