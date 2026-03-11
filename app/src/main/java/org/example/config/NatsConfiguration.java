package org.example.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NatsConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(NatsConfiguration.class);

    @Getter
    @Value("${spring.nats.port}")
    private int natsPort;

    @Getter
    @Value("${spring.nats.host}")
    private String natsHost;

    @PostConstruct
    void logConfig() {
        logger.debug("NatsConfiguration loaded — port={}, host={}", natsPort, natsHost);
    }
}
