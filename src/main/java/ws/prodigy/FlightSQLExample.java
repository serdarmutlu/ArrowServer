package ws.prodigy;

import org.apache.arrow.flight.*;
import org.apache.arrow.memory.RootAllocator;

public class FlightSQLExample {
    public static void main(String[] args) {
        try (
                RootAllocator allocator = new RootAllocator();
                FlightClient client = FlightClient.builder(allocator, Location.forGrpcInsecure("localhost", 8815)).build()
        ) {
            FlightInfo flightInfo = client.getInfo(
                    FlightDescriptor.command("SELECT * FROM casts".getBytes())
            );

            try (FlightStream stream = client.getStream(flightInfo.getEndpoints().get(0).getTicket())) {
                while (stream.next()) {
                    System.out.println(stream.getRoot().contentToTSVString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
