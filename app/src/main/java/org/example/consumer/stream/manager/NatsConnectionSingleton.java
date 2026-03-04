package org.example.consumer.stream.manager;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.example.consumer.config.NatsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
class NatsConnectionSingleton {
    private final Logger logger = LoggerFactory.getLogger(NatsConnectionSingleton.class);
    private final int natsPort;

    @Getter
    private Connection connection;

    @Autowired
    NatsConnectionSingleton(NatsConfiguration cnf) {
        this.natsPort = cnf.getNatsPort();
    }

    @PostConstruct
    void init() throws BeanCreationException {
        logger.debug("Initialising NATS connection to {}", hostName());
        try {
            this.connection = newConn();
            logger.debug("NATS connection established successfully (status: {})", connection.getStatus());
        } catch (Exception e) {
            logger.debug("Failed to establish NATS connection: {}", e.getMessage());
            throw new BeanCreationException("fatal exception occurred creating connection to NATS broker: {}", e.getMessage());
        }
    }

    private Connection newConn() throws IOException, InterruptedException {
        logger.debug("Building NATS options — server: {}, timeout: 5s, maxReconnects: 5, reconnectWait: 1s", hostName());
        Options options = new Options.Builder()
                .server(hostName())
                .connectionTimeout(Duration.ofSeconds(5))
                .maxReconnects(5)
                .reconnectWait(Duration.ofSeconds(1))
                .build();
        logger.debug("Connecting to NATS broker...");
        return Nats.connect(options);
    }

    private String hostName() {
        return String.format("nats://localhost:%d", natsPort);
    }
}
