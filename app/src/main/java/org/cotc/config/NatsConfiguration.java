package org.cotc.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class NatsConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(NatsConfiguration.class);
    @Getter
    private Connection connection;

    @Getter
    @Value("${spring.nats.port}")
    private int natsPort;

    @Getter
    @Value("${spring.nats.host}")
    private String natsHost;

    @PostConstruct
    void init() throws BeanCreationException {
        logger.debug("NatsConfiguration loaded — port={}, host={}", natsPort, natsHost);
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
        return String.format("nats://%s:%d", natsHost, natsPort);
    }
}
