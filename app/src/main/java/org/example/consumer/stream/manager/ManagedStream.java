package org.example.consumer.stream.manager;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import lombok.Builder;
import org.example.consumer.repository.StreamRepository;
import org.example.libb3project.dto.TimeSeriesMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Builder
class ManagedStream {
    private static final Logger logger = LoggerFactory.getLogger(ManagedStream.class);
    private TimeSeriesStreamHandler handler;
    private String natsStreamName;
    private final StreamRepository streamRepository;
    private final Connection natsConnection;
    private boolean isListening;
    private Dispatcher dispatcher;
    private Sinks.Many<TimeSeriesMessageDTO> sink;
    private Flux<TimeSeriesMessageDTO> flux;

    void startListening() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
        this.flux = sink.asFlux();
        logger.debug("startListening - flux initialised");
        handler.setSinkAndFlux(this.sink, this.flux);
        if (isValid()) {
            this.dispatcher = natsConnection.createDispatcher(handler);
            this.dispatcher.subscribe(natsStreamName);
            this.isListening = true;
            logger.debug("startListening - subscribed to NATS subject '{}'", natsStreamName);
        } else {
            logger.error("invalid ");
        }
    }

    private boolean isValid() {
        return flux != null && natsStreamName != null && natsConnection != null;
    }

    Flux<TimeSeriesMessageDTO> getFlux() {
        return this.flux;
    }
}
