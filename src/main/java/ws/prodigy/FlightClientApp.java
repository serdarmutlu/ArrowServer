package ws.prodigy;

import org.apache.arrow.flight.*;
import org.apache.arrow.memory.*;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;

import java.nio.charset.StandardCharsets;

public class FlightClientApp {

    public static void main(String[] args) throws Exception {
        // Ticket belirleniyor (örn: "customers", "products" gibi)
        String ticketName = "products";  // Bunu değiştirerek farklı kaynaklara erişebilirsin

        try (
                BufferAllocator allocator = new RootAllocator();
                org.apache.arrow.flight.FlightClient client = org.apache.arrow.flight.FlightClient.builder(allocator, Location.forGrpcInsecure("localhost", 8815)).build()
        ) {
            System.out.println("🎫 İstenilen veri: " + ticketName);

            // Ticket oluştur
            FlightDescriptor descriptor = FlightDescriptor.path(ticketName);
            FlightInfo info = client.getInfo(descriptor);

            if (info.getEndpoints().isEmpty()) {
                System.err.println("❌ Endpoints boş, geçersiz ticket olabilir.");
                return;
            }

            Ticket ticket = info.getEndpoints().get(0).getTicket();

            // Veri akışını başlat
            try (FlightStream stream = client.getStream(ticket)) {
                VectorSchemaRoot root = stream.getRoot();

                System.out.println("\n📦 Gelen veri:\n--------------------------");

                while (stream.next()) {
                    IntVector idVec = (IntVector) root.getVector("id");
                    VarCharVector nameVec = (VarCharVector) root.getVector("name");

                    for (int i = 0; i < root.getRowCount(); i++) {
                        int id = idVec.get(i);
                        String name = new String(nameVec.get(i), StandardCharsets.UTF_8);
                        System.out.printf("🧾 id: %-5d | name: %s%n", id, name);
                    }
                }

                System.out.println("\n✅ Veri başarıyla alındı.");
            }
        }
    }
}

