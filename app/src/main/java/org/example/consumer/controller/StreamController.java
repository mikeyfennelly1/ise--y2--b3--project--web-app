package org.example.consumer.controller;

import org.example.consumer.stream.StreamingSubsystemFacade;
import org.example.consumer.stream.exception.InvalidSubscriptionTreePathFormatException;
import org.example.consumer.stream.exception.SubscriptionAlreadyExistsException;
import org.example.consumer.stream.exception.TreePathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consumer")
public class StreamController {

    private static final Logger logger = LoggerFactory.getLogger(StreamController.class);

    private final StreamingSubsystemFacade streamingSubsystemFacade;

    @Autowired
    public StreamController(
            StreamingSubsystemFacade streamingSubsystemFacade
    ) {
        this.streamingSubsystemFacade = streamingSubsystemFacade;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.debug("GET /api/consumer/health - hit health endpoint");
        return ResponseEntity.ok(Map.of("msg", "consumer API is healthy"));
    }

    @GetMapping("/streams")
    public List<String> getAllStreams() {
        logger.debug("GET /api/consumer/streams - fetching active subscriptions");
        return streamingSubsystemFacade.getAllStreamNames();
    }

    @PostMapping("/streams")
    public ResponseEntity<?> createNewStream(@RequestBody(required = false) Map<String, String> body) {
        logger.debug("POST /api/consumer/streams - body: {}", body);
        if (hasNameField(body)) {
            logger.debug("POST /api/consumer/streams - rejected: missing or blank 'name' field");
            return ResponseEntity.badRequest().body(Map.of("error", "Request body must include a 'name' field."));
        }
        if (!body.containsKey("parent")) {
            logger.debug("POST /api/consumer/streams - rejected: missing 'parent' field");
            return ResponseEntity.badRequest().body(Map.of("error", "Request body must include a 'parent' field (set to null to create a root node)."));
        }

        try {
            streamingSubsystemFacade.createStream(body.get("name"), body.get("parent"));
        } catch (InvalidSubscriptionTreePathFormatException e) {
            logger.debug("POST /api/consumer/streams - rejected: invalid path format - {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (TreePathNotFoundException e) {
            logger.debug("POST /api/consumer/streams - rejected: parent path not found - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SubscriptionAlreadyExistsException e) {
            logger.debug("POST /api/consumer/streams - rejected: subscription already exists - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
        logger.debug("POST /api/consumer/streams - created subscription name='{}' parent='{}'", body.get("name"), body.get("parent"));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private static boolean hasNameField(Map<String, String> body) {
        return body == null || !body.containsKey("name") || body.get("name") == null || body.get("name").isBlank();
    }

    @GetMapping("/streams/children")
    public ResponseEntity<?> getChildStreams(@RequestParam String stream) {
        logger.debug("GET /api/consumer/streams/children - parent='{}'", stream);
        try {
            List<String> childSubscriptions = streamingSubsystemFacade.getChildStreams(stream);
            return ResponseEntity.ok(childSubscriptions);
        } catch (InvalidSubscriptionTreePathFormatException e) {
            logger.debug("GET /api/consumer/streams/children - rejected: invalid path format - {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (TreePathNotFoundException e) {
            logger.debug("GET /api/consumer/streams/children - rejected: parent path not found - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
