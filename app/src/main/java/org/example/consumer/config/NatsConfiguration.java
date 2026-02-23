package org.example.consumer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.example.consumer.model.device.SysinfoMessage;
import org.example.consumer.repository.TimeseriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class NatsConfiguration {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(NatsConfiguration.class);

    @Value("${spring.nats.port}")
    private int natsPort;

    @Getter
    @Value("${spring.nats.topic-name}")
    private String sysinfoSubject;

    @Getter
    private Connection connection;

    private final TimeseriesRepository sysinfoMessageRepository;

    public NatsConfiguration(TimeseriesRepository repository) {
        this.sysinfoMessageRepository = repository;
    }

    @PostConstruct
    void init() throws BeanCreationException {
        try {
            this.connection = newConn();
            this.connection.createDispatcher().subscribe(sysinfoSubject, this::handleNewMessage);
        } catch (Exception e) {
            throw new BeanCreationException("fatal exception occurred creating connection to NATS broker: {}", e.getMessage());
        }
    }

    private Connection newConn() throws IOException, InterruptedException {
        Options options = new Options.Builder()
                .server(hostName())
                .connectionTimeout(Duration.ofSeconds(5))
                .maxReconnects(5)
                .reconnectWait(Duration.ofSeconds(1))
                .build();
        return Nats.connect(options);
    }

    private void handleNewMessage(Message message) {
        try {
            var msg = jsonMapper.readValue(new String(message.getData(), StandardCharsets.UTF_8), SysinfoMessage.class);
            sysinfoMessageRepository.save(msg);
        } catch (IOException e) {
            logger.error("failed to deserialize message from subject '{}': {}", sysinfoSubject, e.getMessage());
        }
    }

    private String hostName() {
        return String.format("nats://localhost:%d", natsPort);
    }
}
