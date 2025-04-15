package ws.prodigy;

import org.apache.arrow.flight.*;
import org.apache.arrow.memory.*;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.pojo.Field;

import java.util.List;

public class DynamicFlightClientApp {

    public static void main(String[] args) throws Exception {
        String ticketName = "customer"; // veya "orders", "products", vs.

        try (
                BufferAllocator allocator = new RootAllocator();
                FlightClient client = FlightClient.builder(allocator, Location.forGrpcInsecure("localhost", 8815)).build()
        ) {
            FlightDescriptor descriptor = FlightDescriptor.path(ticketName);
            FlightInfo info = client.getInfo(descriptor);

            if (info.getEndpoints().isEmpty()) {
                System.err.println("❌ Ticket bulunamadı: " + ticketName);
                return;
            }

            Ticket ticket = info.getEndpoints().get(0).getTicket();

            try (FlightStream stream = client.getStream(ticket)) {
                VectorSchemaRoot root = stream.getRoot();
                List<Field> fields = root.getSchema().getFields();

                System.out.println("📦 Gelen kolonlar:");
                for (Field field : fields) {
                    System.out.printf("  • %s (%s)%n", field.getName(), field.getType());
                }

                System.out.println("\n📋 Satır verileri:");
                while (stream.next()) {
                    for (int i = 0; i < root.getRowCount(); i++) {
                        System.out.print("🔸 Satır " + (i + 1) + ": ");
                        for (FieldVector vector : root.getFieldVectors()) {
                            Object value = vector.getObject(i);
                            System.out.print(value + " | ");
                        }
                        System.out.println();
                    }
                }

                System.out.println("\n✅ Veri dinamik olarak başarıyla alındı.");
            }
        }
    }
}
