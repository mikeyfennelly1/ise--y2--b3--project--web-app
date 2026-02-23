package org.example.consumer.subscriber;

import io.nats.client.Dispatcher;
import jakarta.annotation.PostConstruct;
import org.example.consumer.config.NatsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.nats.client.Connection;

import java.nio.charset.StandardCharsets;

@Service
public class SysinfoEventSubscriber {
    private final Logger log = LoggerFactory.getLogger(SysinfoEventSubscriber.class);
    private final Connection natsConnection;
    private static final String SUBJECT = "desktop-sysinfo";

    @Autowired
    SysinfoEventSubscriber(NatsConfiguration natsConnection) {
        this.natsConnection = natsConnection.getConnection();
    }

    @PostConstruct
    public void subscribe() {
        Dispatcher dispatcher = natsConnection.createDispatcher(message -> {
            String payload = new String(message.getData(), StandardCharsets.UTF_8);
            log.info("Received message on [{}]: {}", SUBJECT, payload);
        });

        dispatcher.subscribe(SUBJECT);

        log.info("Subscribed to NATS subject [{}]", SUBJECT);
    }
}
