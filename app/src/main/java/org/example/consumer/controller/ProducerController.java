package org.example.consumer.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.example.consumer.model.Producer;
import org.example.consumer.model.Stream;
import org.example.consumer.repository.ProducerRepository;
import org.example.consumer.repository.StreamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/consumer")
public class ProducerController {

    private static final Logger logger = LoggerFactory.getLogger(ProducerController.class);

    private final ProducerRepository producerRepository;
    private final StreamRepository streamRepository;

    @Autowired
    public ProducerController(ProducerRepository producerRepository, StreamRepository streamRepository) {
        this.producerRepository = producerRepository;
        this.streamRepository = streamRepository;
    }

    @Operation(summary = "Create a producer", description = "Registers a new named producer and associates it with an existing stream. Requires 'name' and 'streamName' fields.")
    @PostMapping("/producers")
    public ResponseEntity<?> createProducer(@RequestBody(required = false) Map<String, String> body) {
        logger.debug("POST /api/consumer/producers - body: {}", body);

        if (body == null || !body.containsKey("name") || body.get("name") == null || body.get("name").isBlank()) {
            logger.debug("POST /api/consumer/producers - rejected: missing or blank 'name' field");
            return ResponseEntity.badRequest().body(Map.of("error", "Request body must include a 'name' field."));
        }
        if (!body.containsKey("streamName") || body.get("streamName") == null || body.get("streamName").isBlank()) {
            logger.debug("POST /api/consumer/producers - rejected: missing or blank 'streamName' field");
            return ResponseEntity.badRequest().body(Map.of("error", "Request body must include a 'streamName' field."));
        }

        String name = body.get("name");
        String streamName = body.get("streamName");

        if (!streamRepository.streamExists(streamName)) {
            logger.debug("POST /api/consumer/producers - rejected: stream '{}' not found", streamName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Stream '" + streamName + "' not found."));
        }

        if (producerRepository.findByName(name) != null) {
            logger.debug("POST /api/consumer/producers - rejected: producer '{}' already exists", name);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Producer '" + name + "' already exists."));
        }

        Stream stream = streamRepository.findByName(streamName);

        Producer producer = Producer.builder()
                .name(name)
                .stream(stream)
                .build();

        Producer saved = producerRepository.save(producer);
        logger.debug("POST /api/consumer/producers - created producer name='{}' stream='{}'", name, streamName);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toDTO());
    }
}
