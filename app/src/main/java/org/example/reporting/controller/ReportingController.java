package org.example.reporting.controller;

import org.example.consumer.model.dto.ProducerDTO;
import org.example.consumer.model.dto.StreamDTO;
import org.example.consumer.model.dto.TimeSeriesRecordDTO;
import org.example.reporting.service.ReportingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reporting")
public class ReportingController {

    private static final Logger logger = LoggerFactory.getLogger(ReportingController.class);

    private final ReportingService reportingService;

    @Autowired
    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
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

    @GetMapping("/streams/{streamId}")
    public ResponseEntity<List<TimeSeriesRecordDTO>> getRecordsByStreamId(@PathVariable Long streamId) {
        logger.debug("GET /api/reporting/streams/{} - fetching time series records", streamId);
        List<TimeSeriesRecordDTO> records = reportingService.getRecordsByStreamId(streamId);
        logger.debug("GET /api/reporting/streams/{} - returning {} record(s)", streamId, records.size());
        logger.trace("GET /api/reporting/streams/{} - payload: {}", streamId, records);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/streams/{streamId}/producers")
    public ResponseEntity<List<ProducerDTO>> getProducersByStreamId(@PathVariable Long streamId) {
        logger.debug("GET /api/reporting/streams/{}/producers - fetching producers", streamId);
        List<ProducerDTO> producers = reportingService.getProducersByStreamId(streamId);
        logger.debug("GET /api/reporting/streams/{}/producers - returning {} producer(s)", streamId, producers.size());
        logger.trace("GET /api/reporting/streams/{}/producers - payload: {}", streamId, producers);
        return ResponseEntity.ok(producers);
    }
}
