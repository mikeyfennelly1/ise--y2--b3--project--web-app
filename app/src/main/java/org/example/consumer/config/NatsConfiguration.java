package org.example.consumer.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NatsConfiguration {
    @Getter
    @Value("${spring.nats.port}")
    private int natsPort;
}
