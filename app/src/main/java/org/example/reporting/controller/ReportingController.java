package org.example.reporting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.consumer.stream.manager.StreamManager;
import org.example.libb3project.dto.ProducerDTO;
import org.example.libb3project.dto.StreamDTO;
import org.example.libb3project.dto.TimeSeriesMessageDTO;
import org.example.libb3project.dto.TimeSeriesRecordDTO;
import org.example.reporting.service.ReportingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reporting")
public class ReportingController {

    private static final Logger logger = LoggerFactory.getLogger(ReportingController.class);

    private final ReportingService reportingService;
    private final StreamManager streamManager;
    private final ObjectMapper objectMapper;

    @Autowired
    public ReportingController(ReportingService reportingService, StreamManager streamManager, ObjectMapper objectMapper) {
        this.reportingService = reportingService;
        this.streamManager = streamManager;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.debug("GET /api/reporting/health - hit health endpoint");
        return ResponseEntity.ok(Map.of("msg", "reporting API is healthy"));
    }

    @GetMapping("/streams")
    public ResponseEntity<List<StreamDTO>> getAllStreams() {
        logger.debug("GET /api/reporting/streams - fetching all streams");
        List<StreamDTO> streams = reportingService.getAllStreams();
        logger.debug("GET /api/reporting/streams - returning {} stream(s)", streams.size());
        logger.trace("GET /api/reporting/streams - payload: {}", streams);
        return ResponseEntity.ok(streams);
    }

    @GetMapping("/streams/hierarchy")
    public ResponseEntity<List<StreamDTO>> getStreamHierarchy() {
        logger.debug("GET /api/reporting/streams/hierarchy - building stream hierarchy");
        List<StreamDTO> hierarchy = reportingService.getStreamHierarchy();
        logger.debug("GET /api/reporting/streams/hierarchy - returning {} root stream(s)", hierarchy.size());
        logger.trace("GET /api/reporting/streams/hierarchy - payload: {}", hierarchy);
        return ResponseEntity.ok(hierarchy);
    }

    @GetMapping(value = "/streams/{streamId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRecordsByStreamId(@PathVariable UUID streamId) {
        logger.debug("GET /api/reporting/streams/{} - opening SSE stream", streamId);
        SseEmitter emitter = new SseEmitter(0L);

        // Send historical records as the first event
        List<TimeSeriesRecordDTO> history = reportingService.getRecordsByStreamId(streamId);
        logger.debug("GET /api/reporting/streams/{} - sending {} historical record(s)", streamId, history.size());
        try {
            emitter.send(SseEmitter.event().name("history").data(history, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            logger.error("GET /api/reporting/streams/{} - failed to send historical data: {}", streamId, e.getMessage());
            emitter.completeWithError(e);
            return emitter;
        }

        // Resolve NATS subject (stream name) and subscribe for live events
        String streamName = reportingService.getStreamNameById(streamId);
        if (streamName == null) {
            logger.warn("GET /api/reporting/streams/{} - stream not found, closing SSE", streamId);
            emitter.complete();
            return emitter;
        }

        AutoCloseable subscription = streamManager.subscribeToStream(streamName, bytes -> {
            try {
                TimeSeriesMessageDTO msg = objectMapper.readValue(bytes, TimeSeriesMessageDTO.class);
                Instant readTime = Instant.ofEpochSecond(msg.getReadTime());
                List<TimeSeriesRecordDTO> liveRecords = msg.getValues().entrySet().stream()
                        .map(e -> new TimeSeriesRecordDTO(null, e.getKey(), e.getValue().floatValue(), msg.getProducerName(), readTime))
                        .toList();
                logger.trace("GET /api/reporting/streams/{} - pushing {} live record(s)", streamId, liveRecords.size());
                emitter.send(SseEmitter.event().name("live").data(liveRecords, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                logger.error("GET /api/reporting/streams/{} - error pushing live event: {}", streamId, e.getMessage());
            }
        });

        emitter.onCompletion(() -> {
            logger.debug("GET /api/reporting/streams/{} - SSE completed, unsubscribing", streamId);
            try { subscription.close(); } catch (Exception ignored) {}
        });
        emitter.onTimeout(() -> {
            logger.debug("GET /api/reporting/streams/{} - SSE timed out, unsubscribing", streamId);
            try { subscription.close(); } catch (Exception ignored) {}
            emitter.complete();
        });
        emitter.onError(e -> {
            logger.debug("GET /api/reporting/streams/{} - SSE error, unsubscribing: {}", streamId, e.getMessage());
            try { subscription.close(); } catch (Exception ignored) {}
        });

        logger.debug("GET /api/reporting/streams/{} - SSE stream open, subscribed to NATS subject '{}'", streamId, streamName);
        return emitter;
    }

    @GetMapping("/streams/{streamId}/producers")
    public ResponseEntity<List<ProducerDTO>> getProducersByStreamId(@PathVariable UUID streamId) {
        logger.debug("GET /api/reporting/streams/{}/producers - fetching producers", streamId);
        List<ProducerDTO> producers = reportingService.getProducersByStreamId(streamId);
        logger.debug("GET /api/reporting/streams/{}/producers - returning {} producer(s)", streamId, producers.size());
        logger.trace("GET /api/reporting/streams/{}/producers - payload: {}", streamId, producers);
        return ResponseEntity.ok(producers);
    }
}
