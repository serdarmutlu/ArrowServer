package ws.prodigy;

import org.apache.arrow.flight.*;
import org.apache.arrow.vector.VectorSchemaRoot;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class CachedFlightProducer extends NoOpFlightProducer {

    private final FlightCacheManager cache;

    public CachedFlightProducer(FlightCacheManager cache) {
        this.cache = cache;
    }

    @Override
    public FlightInfo getFlightInfo(CallContext context, FlightDescriptor descriptor) {
        String ticketText = descriptor.getPath().get(0); // örn: "customers"
        VectorSchemaRoot root = cache.get(ticketText);
        return new FlightInfo(root.getSchema(), descriptor,
                List.of(new FlightEndpoint(
                        new Ticket(ticketText.getBytes(StandardCharsets.UTF_8)),
                        Location.forGrpcInsecure("localhost", 8815))),
                -1, -1);
    }

    @Override
    public void getStream(CallContext context, Ticket ticket, ServerStreamListener listener) {
        String key = new String(ticket.getBytes(), StandardCharsets.UTF_8);
        VectorSchemaRoot root = cache.get(key);
        listener.start(root);
        listener.putNext();
        listener.completed();
    }
}
