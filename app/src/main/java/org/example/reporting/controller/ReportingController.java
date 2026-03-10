package org.example.reporting.controller;

import org.example.libb3project.dto.ProducerDTO;
import org.example.libb3project.dto.StreamDTO;
import org.example.reporting.service.ReportingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    public ResponseEntity<?> getStreams(@RequestParam(required = false) String name) {
        if (name != null) {
            logger.debug("GET /api/reporting/streams?name={} - fetching stream by name", name);
            return reportingService.getStreamByName(name)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        logger.debug("GET /api/reporting/streams?name={} - stream not found", name);
                        return ResponseEntity.notFound().build();
                    });
        }
        logger.debug("GET /api/reporting/streams - building stream hierarchy");
        List<StreamDTO> hierarchy = reportingService.getStreamHierarchy();
        logger.debug("GET /api/reporting/streams - returning {} root stream(s)", hierarchy.size());
        logger.trace("GET /api/reporting/streams - payload: {}", hierarchy);
        return ResponseEntity.ok(hierarchy);
    }

    @GetMapping("/producers")
    public ResponseEntity<?> getProducers(@RequestParam(required = false) String name) {
        if (name != null) {
            logger.debug("GET /api/reporting/producers?name={} - fetching producer by name", name);
            return reportingService.getProducerByName(name)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        logger.debug("GET /api/reporting/producers?name={} - producer not found", name);
                        return ResponseEntity.notFound().build();
                    });
        }
        logger.debug("GET /api/reporting/producers - fetching all producers");
        List<ProducerDTO> producers = reportingService.getAllProducers();
        logger.debug("GET /api/reporting/producers - returning {} producer(s)", producers.size());
        return ResponseEntity.ok(producers);
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
