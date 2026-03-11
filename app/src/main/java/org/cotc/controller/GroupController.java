package org.cotc.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.cotc.exception.GroupAlreadyExistsException;
import org.cotc.repository.TimeseriesRepository;
import org.cotc.exception.InvalidGroupNameException;
import org.cotc.exception.GroupNotFoundException;
import org.cotc.libcotc.dto.TimeSeriesMessageDTO;
import org.cotc.libcotc.dto.TimeSeriesRecordDTO;
import org.cotc.service.GroupService;
import org.cotc.service.TimeSeriesRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    private static final Logger logger = LoggerFactory.getLogger(GroupController.class);
    private final TimeseriesRepository timeseriesRepository;
    private final TimeSeriesRecordService timeSeriesRecordService;

    @Autowired
    public GroupController(
            TimeseriesRepository timeseriesRepository,
            GroupService groupService, TimeSeriesRecordService timeSeriesRecordService) {
        this.timeseriesRepository = timeseriesRepository;
        this.groupService = groupService;
        this.timeSeriesRecordService = timeSeriesRecordService;
    }

    @Operation(summary = "Health check", description = "Returns a status message confirming the groups API is running.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        logger.debug("GET /api/groups/health - hit health endpoint");
        return ResponseEntity.ok(Map.of("msg", "groups API is healthy"));
    }

    @Operation(summary = "List all groups", description = "Returns a list of all groups.")
    @GetMapping("/")
    public List<String> getAllGroups() {
        logger.debug("GET /api/groups/ - fetching active groupss");
        return groupService.getAllGroupNames();
    }

    @Operation(summary = "Create a group", description = "Creates a new named group. Requires a 'name' field and a 'parent' field (set parent to null for a root group).")
    @PostMapping("")
    public ResponseEntity<?> createNewGroup(@RequestParam(required = true) String groupName) throws GroupAlreadyExistsException, GroupNotFoundException, InvalidGroupNameException {
        groupService.createGroup(groupName);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private static boolean hasNameField(Map<String, String> body) {
        return body == null || !body.containsKey("name") || body.get("name") == null || body.get("name").isBlank();
    }

    @Operation(summary = "Delete a group", description = "Deletes the group with the given name.")
    @DeleteMapping("")
    public ResponseEntity<?> deleteGroup(@RequestParam String name) throws GroupNotFoundException {
        logger.debug("DELETE /api/groups/ - name='{}'", name);
        groupService.deleteGroup(name);
        logger.debug("DELETE /api/groups/ - group '{}' deleted successfully", name);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Subscribe to group events", description = "Opens a Server-Sent Events connection and  time-series records for the given group.")
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<?>> subscribeToGroupEvents(@RequestParam String group) throws GroupNotFoundException {
        logger.debug("GET /api/groups//events - group='{}'", group);
        if (!groupService.groupExists(group)) {
            logger.debug("GET /api/groups//events - rejected: group '{}' not found", group);
            throw new GroupNotFoundException(group);
        }

        List<TimeSeriesRecordDTO> history = timeseriesRepository.findByProducerGroupName(group)
                .stream()
                .map(r -> r.toDTO())
                .collect(Collectors.toList());
        logger.debug("GET /api/groups//events - sending {} history record(s) for group '{}'", history.size(), group);

        Flux<ServerSentEvent<?>> historyFlux = Flux.just(
                ServerSentEvent.<List<TimeSeriesRecordDTO>>builder()
                        .event("history")
                        .data(history)
                        .build()
        );

        Flux<ServerSentEvent<?>> liveFlux = groupService.getGroupFlux(group)
                .map(dto -> ServerSentEvent.<TimeSeriesMessageDTO>builder()
                        .event("update")
                        .data(dto.data())
                        .build()
                );

        return historyFlux.concatWith(liveFlux);
    }

    @Operation(summary = "Get child ", description = "Returns a list of direct child group names for the given parent group.")
    @GetMapping("/children")
    public ResponseEntity<?> getSubgroups(@RequestParam String group) throws GroupNotFoundException, InvalidGroupNameException {
        logger.debug("GET /api/groups//children - parent='{}'", group);
        List<String> childSubscriptions = groupService.getDescendantGroups(group);
        return ResponseEntity.ok(childSubscriptions);
    }
}
