package org.cotc.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.cotc.exception.GroupNotFoundException;
import org.cotc.libcotc.dto.ProducerDTO;
import org.cotc.model.Group;
import org.cotc.model.Producer;
import org.cotc.service.GroupService;
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

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/producer")
public class ProducerController {

    private static final Logger logger = LoggerFactory.getLogger(ProducerController.class);

    private final ProducerService producerService;
    private final GroupRepository groupRepository;
    private final GroupService groupService;

    @Autowired
    public ProducerController(ProducerService producerService, GroupRepository groupRepository, GroupService groupService) {
        this.producerService = producerService;
        this.groupRepository = groupRepository;
        this.groupService = groupService;
    }


    public record ProducerCreationDTO(String name, String group) {}
    @Operation(summary = "Create a producer", description = "Registers a new named producer and associates it with an existing group. Requires a ProducerDTO body with 'name' and 'group' fields.")
    @PostMapping("")
    public ResponseEntity<?> createProducer(@RequestBody(required = false) ProducerCreationDTO body) throws GroupNotFoundException {
        logger.debug("POST /api/producer - body: {}", body.toString());

        if (!groupService.groupExists(body.group())) {
            logger.debug("POST /api/producer - rejected: group '{}' not found", body.group());
            throw new GroupNotFoundException(body.group());
        }

        if (producerService.getProducerByName(body.name()) != null) {
            logger.debug("POST /api/producer - rejected: producer '{}' already exists", body.name());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Producer '" + body.name() + "' already exists."));
        }

        Group group = groupRepository.findByName(body.group());

        Producer producer = Producer.builder()
                .name(body.name())
                .group(group)
                .build();

        ProducerDTO saved = producerService.createNewProducer(producer);
        logger.debug("POST /api/producer - created producer name='{}' group='{}'", body.name(), body.group);
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

    @Operation(summary = "Delete a producer", description = "Deletes the producer with the given name.")
    @DeleteMapping("")
    public ResponseEntity<?> deleteProducer(@RequestParam String name) {
        logger.debug("DELETE /api/producer?name={}", name);
        boolean deleted = producerService.deleteProducerByName(name);
        if (!deleted) {
            logger.debug("DELETE /api/producer?name={} - not found", name);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Producer '" + name + "' not found."));
        }
        logger.debug("DELETE /api/producer?name={} - deleted", name);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get producers by group", description = "Returns all producers associated with the given group.")
    @GetMapping("/{groupName}")
    public ResponseEntity<List<ProducerDTO>> getProducersByGroupId(@PathVariable String groupName) {
        logger.debug("GET /api/reporting/streams/{}/producers - fetching producers", groupName);
        List<ProducerDTO> producers = producerService.getAllProducerDtosForGroup(groupName);
        logger.debug("GET /api/reporting/streams/{}/producers - returning {} producer(s)", groupName, producers.size());
        logger.trace("GET /api/reporting/streams/{}/producers - payload: {}", groupName, producers);
        return ResponseEntity.ok(producers);
    }
}
