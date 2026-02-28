package org.example.consumer.config;

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

    @PostConstruct
    void logConfig() {
        logger.debug("NatsConfiguration loaded — port: {}", natsPort);
    }
}
