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
        try {
            String ticketName = descriptor.getPath().get(0);
            cache.loadIfMissing(ticketName); // lazily load if needed
            VectorSchemaRoot root = cache.get(ticketName);

            return new FlightInfo(
                    root.getSchema(),
                    descriptor,
                    List.of(new FlightEndpoint(
                            new Ticket(ticketName.getBytes(StandardCharsets.UTF_8)),
                            Location.forGrpcInsecure("localhost", 8815)
                    )),
                    -1, -1
            );
        } catch (Exception e) {
            throw CallStatus.INTERNAL.withDescription("FlightInfo error: " + e.getMessage()).toRuntimeException();
        }
    }

    @Override
    public void getStream(CallContext context, Ticket ticket, ServerStreamListener listener) {
        try {
            String ticketName = new String(ticket.getBytes(), StandardCharsets.UTF_8);
            cache.loadIfMissing(ticketName);
            VectorSchemaRoot root = cache.get(ticketName);

            listener.start(root);
            listener.putNext();
            listener.completed();
        } catch (Exception e) {
            listener.error(CallStatus.INTERNAL.withDescription("Stream error: " + e.getMessage()).toRuntimeException());
        }
    }
}
