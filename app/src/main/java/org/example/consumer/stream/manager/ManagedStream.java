package org.example.consumer.stream.manager;

import io.nats.client.MessageHandler;
import lombok.Builder;
import org.example.consumer.repository.StreamRepository;
import org.example.libb3project.dto.TimeSeriesRecordDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

@Builder
class ManagedStream {
    private static final Logger logger = LoggerFactory.getLogger(ManagedStream.class);
    private MessageHandler natsMessageHandler;
    private Flux<TimeSeriesRecordDTO> flux;
    private String natsStreamName;
    private final StreamRepository streamRepository;
    private boolean isListening;

    void startListening() {
        if (isValid()) {
            // attach dispatcher to NATS
            //
            this.isListening = true;
        }
    }

    private boolean isValid() {
        return flux != null && natsStreamName != null;
    }

    Flux<TimeSeriesRecordDTO> getFlux() {
        return this.flux;
    }
}
