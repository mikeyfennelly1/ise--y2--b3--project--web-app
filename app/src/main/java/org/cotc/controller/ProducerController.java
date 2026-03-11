package org.cotc.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.cotc.libcotc.dto.ProducerDTO;
import org.cotc.model.Group;
import org.cotc.model.Producer;
import org.cotc.service.ProducerService;
import org.cotc.repository.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/producers")
public class ProducerController {

    private static final Logger logger = LoggerFactory.getLogger(ProducerController.class);

    private final ProducerService producerService;
    private final GroupRepository groupRepository;

    @Autowired
    public ProducerController(ProducerService producerService, GroupRepository groupRepository) {
        this.producerService = producerService;
        this.groupRepository = groupRepository;
    }

    @Operation(summary = "Create a producer", description = "Registers a new named producer and associates it with an existing stream. Requires 'name' and 'streamName' fields.")
    @PostMapping("")
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

        if (!groupRepository.groupExists(streamName)) {
            logger.debug("POST /api/consumer/producers - rejected: stream '{}' not found", streamName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Stream '" + streamName + "' not found."));
        }

        if (producerService.getProducerByName(name) != null) {
            logger.debug("POST /api/consumer/producers - rejected: producer '{}' already exists", name);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Producer '" + name + "' already exists."));
        }

        Group group = groupRepository.findByName(streamName);

        Producer producer = Producer.builder()
                .name(name)
                .group(group)
                .build();

        ProducerDTO saved = producerService.createNewProducer(producer);
        logger.debug("POST /api/consumer/producers - created producer name='{}' stream='{}'", name, streamName);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Get producers", description = "Returns all producers. If the optional 'name' query param is provided, returns the single matching producer instead.")
    @GetMapping("")
    public ResponseEntity<?> getProducers(@RequestParam(required = false) String name) {
        if (name != null) {
            logger.debug("GET /api/reporting/producers?name={} - fetching producer by name", name);
            return producerService.getProducerDtoByName(name)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        logger.debug("GET /api/reporting/producers?name={} - producer not found", name);
                        return ResponseEntity.notFound().build();
                    });
        }
        logger.debug("GET /api/reporting/producers - fetching all producers");
        List<ProducerDTO> producers = producerService.getAllProducerDtos();
        logger.debug("GET /api/reporting/producers - returning {} producer(s)", producers.size());
        return ResponseEntity.ok(producers);
    }

    @Operation(summary = "Get producers by stream", description = "Returns all producers associated with the given stream ID.")
    @GetMapping("/{groupId}")
    public ResponseEntity<List<ProducerDTO>> getProducersByGroupId(@PathVariable UUID groupId) {
        logger.debug("GET /api/reporting/streams/{}/producers - fetching producers", groupId);
        List<ProducerDTO> producers = producerService.getProducerDtosByGroupUuid(groupId);
        logger.debug("GET /api/reporting/streams/{}/producers - returning {} producer(s)", groupId, producers.size());
        logger.trace("GET /api/reporting/streams/{}/producers - payload: {}", groupId, producers);
        return ResponseEntity.ok(producers);
    }
}
