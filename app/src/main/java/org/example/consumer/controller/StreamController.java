package org.example.consumer.controller;

import org.example.consumer.stream.exception.InvalidStreamNameException;
import org.example.consumer.stream.exception.StreamAlreadyExistsException;
import org.example.consumer.stream.exception.StreamNotFoundException;
import org.example.consumer.stream.manager.StreamManager;
import org.example.consumer.stream.manager.StreamManagerFactory;
import org.example.libb3project.dto.TimeSeriesRecordDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consumer")
public class StreamController {

    private static final Logger logger = LoggerFactory.getLogger(StreamController.class);
    private final StreamManager streamManager;

    @Autowired
    public StreamController(
            StreamManagerFactory managerFactory
    ) {
        this.streamManager = managerFactory.getManager("simple");
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.debug("GET /api/consumer/health - hit health endpoint");
        return ResponseEntity.ok(Map.of("msg", "consumer API is healthy"));
    }

    @GetMapping("/streams")
    public List<String> getAllStreams() {
        logger.debug("GET /api/consumer/streams - fetching active subscriptions");
        return streamManager.getAllStreamNames();
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
        String streamName = body.get("name");
        String streamParentName = body.get("parent");
        try {
            streamManager.createStream(streamName, streamParentName);
        } catch (InvalidStreamNameException e) {
            logger.debug("POST /api/consumer/streams - rejected: invalid path format - {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (StreamNotFoundException e) {
            logger.debug("POST /api/consumer/streams - rejected: parent path not found - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (StreamAlreadyExistsException e) {
            logger.debug("POST /api/consumer/streams - rejected: stream already exists - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
        logger.debug("POST /api/consumer/streams - created subscription name='{}' parent='{}'", body.get("name"), body.get("parent"));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private static boolean hasNameField(Map<String, String> body) {
        return body == null || !body.containsKey("name") || body.get("name") == null || body.get("name").isBlank();
    }

    @DeleteMapping("/streams")
    public ResponseEntity<?> deleteStream(@RequestParam String name) {
        logger.debug("DELETE /api/consumer/streams - name='{}'", name);
        try {
            streamManager.deleteStream(name);
        } catch (StreamNotFoundException e) {
            logger.debug("DELETE /api/consumer/streams - rejected: stream not found - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
        logger.debug("DELETE /api/consumer/streams - stream '{}' deleted successfully", name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/streams/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> subscribeToStreamEvents(@RequestParam String stream) {
        logger.debug("GET /api/consumer/streams/events - stream='{}'", stream);
        if (!streamManager.streamAlreadyExists(stream)) {
            logger.debug("GET /api/consumer/streams/events - rejected: stream '{}' not found", stream);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Stream '" + stream + "' not found."));
        }
        Flux<TimeSeriesRecordDTO> flux = streamManager.getStreamSSESink(stream);
        return ResponseEntity.ok(flux);
    }

    @GetMapping("/streams/children")
    public ResponseEntity<?> getChildStreams(@RequestParam String stream) {
        logger.debug("GET /api/consumer/streams/children - parent='{}'", stream);
        try {
            List<String> childSubscriptions = streamManager.getChildStreams(stream);
            return ResponseEntity.ok(childSubscriptions);
        } catch (InvalidStreamNameException e) {
            logger.debug("GET /api/consumer/streams/children - rejected: invalid path format - {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (StreamNotFoundException e) {
            logger.debug("GET /api/consumer/streams/children - rejected: parent path not found - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
