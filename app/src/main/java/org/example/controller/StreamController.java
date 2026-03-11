package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.example.repository.TimeseriesRepository;
import org.example.stream.exception.InvalidStreamNameException;
import org.example.stream.exception.StreamAlreadyExistsException;
import org.example.stream.exception.StreamNotFoundException;
import org.example.stream.manager.StreamManager;
import org.example.stream.manager.StreamManagerFactory;
import org.example.libb3project.dto.TimeSeriesMessageDTO;
import org.example.libb3project.dto.TimeSeriesRecordDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/consumer")
public class StreamController {

    public record ErrorResponse(String error) {}

    private static final Logger logger = LoggerFactory.getLogger(StreamController.class);
    private final StreamManager streamManager;
    private final TimeseriesRepository timeseriesRepository;

    @Autowired
    public StreamController(
            StreamManagerFactory managerFactory,
            TimeseriesRepository timeseriesRepository
    ) {
        this.streamManager = managerFactory.getManager("simple");
        this.timeseriesRepository = timeseriesRepository;
    }

    @Operation(summary = "Health check", description = "Returns a status message confirming the consumer API is running.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.debug("GET /api/consumer/health - hit health endpoint");
        return ResponseEntity.ok(Map.of("msg", "consumer API is healthy"));
    }

    @Operation(summary = "List all streams", description = "Returns a list of all active stream names.")
    @GetMapping("/streams")
    public List<String> getAllStreams() {
        logger.debug("GET /api/consumer/streams - fetching active subscriptions");
        return streamManager.getAllStreamNames();
    }

    @Operation(summary = "Create a stream", description = "Creates a new named stream. Requires a 'name' field and a 'parent' field (set parent to null for a root stream).")
    @PostMapping("/streams")
    public ResponseEntity<ErrorResponse> createNewStream(@RequestBody(required = false) Map<String, String> body) {
        logger.debug("POST /api/consumer/streams - body: {}", body);
        if (hasNameField(body)) {
            logger.debug("POST /api/consumer/streams - rejected: missing or blank 'name' field");
            return ResponseEntity.badRequest().body(new ErrorResponse("Request body must include a 'name' field."));
        }
        if (!body.containsKey("parent")) {
            logger.debug("POST /api/consumer/streams - rejected: missing 'parent' field");
            return ResponseEntity.badRequest().body(new ErrorResponse("Request body must include a 'parent' field (set to null to create a root node)."));
        }
        String streamName = body.get("name");
        String streamParentName = body.get("parent");
        try {
            streamManager.createStream(streamName, streamParentName);
        } catch (InvalidStreamNameException e) {
            logger.debug("POST /api/consumer/streams - rejected: invalid path format - {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (StreamNotFoundException e) {
            logger.debug("POST /api/consumer/streams - rejected: parent path not found - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (StreamAlreadyExistsException e) {
            logger.debug("POST /api/consumer/streams - rejected: stream already exists - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        }
        logger.debug("POST /api/consumer/streams - created subscription name='{}' parent='{}'", body.get("name"), body.get("parent"));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private static boolean hasNameField(Map<String, String> body) {
        return body == null || !body.containsKey("name") || body.get("name") == null || body.get("name").isBlank();
    }

    @Operation(summary = "Delete a stream", description = "Deletes the stream with the given name.")
    @DeleteMapping("/streams")
    public ResponseEntity<ErrorResponse> deleteStream(@RequestParam String name) {
        logger.debug("DELETE /api/consumer/streams - name='{}'", name);
        try {
            streamManager.deleteStream(name);
        } catch (StreamNotFoundException e) {
            logger.debug("DELETE /api/consumer/streams - rejected: stream not found - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        }
        logger.debug("DELETE /api/consumer/streams - stream '{}' deleted successfully", name);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Subscribe to stream events", description = "Opens a Server-Sent Events connection and streams time-series records for the given stream.")
    @GetMapping(value = "/streams/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<?>> subscribeToStreamEvents(@RequestParam String stream) {
        logger.debug("GET /api/consumer/streams/events - stream='{}'", stream);
        if (!streamManager.streamAlreadyExists(stream)) {
            logger.debug("GET /api/consumer/streams/events - rejected: stream '{}' not found", stream);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stream '" + stream + "' not found.");
        }

        List<TimeSeriesRecordDTO> history = timeseriesRepository.findByProducerStreamName(stream)
                .stream()
                .map(r -> r.toDTO())
                .collect(Collectors.toList());
        logger.debug("GET /api/consumer/streams/events - sending {} history record(s) for stream '{}'", history.size(), stream);

        Flux<ServerSentEvent<?>> historyFlux = Flux.just(
                ServerSentEvent.<List<TimeSeriesRecordDTO>>builder()
                        .event("history")
                        .data(history)
                        .build()
        );

        Flux<ServerSentEvent<?>> liveFlux = streamManager.getStreamSSESink(stream)
                .map(dto -> ServerSentEvent.<TimeSeriesMessageDTO>builder()
                        .event("update")
                        .data(dto)
                        .build());

        return historyFlux.concatWith(liveFlux);
    }

    @Operation(summary = "Get child streams", description = "Returns a list of direct child stream names for the given parent stream.")
    @GetMapping("/streams/children")
    public ResponseEntity<?> getChildStreams(@RequestParam String stream) {
        logger.debug("GET /api/consumer/streams/children - parent='{}'", stream);
        try {
            List<String> childSubscriptions = streamManager.getChildStreams(stream);
            return ResponseEntity.ok(childSubscriptions);
        } catch (InvalidStreamNameException e) {
            logger.debug("GET /api/consumer/streams/children - rejected: invalid path format - {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (StreamNotFoundException e) {
            logger.debug("GET /api/consumer/streams/children - rejected: parent path not found - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        }
    }
}
