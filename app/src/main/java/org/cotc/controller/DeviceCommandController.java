package org.cotc.controller;

import org.cotc.service.DeviceCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/consumer/commands")
public class DeviceCommandController {
    private static final Logger logger = LoggerFactory.getLogger(DeviceCommandController.class);
    private final DeviceCommandService deviceCommandService;
    @Autowired
    public DeviceCommandController(DeviceCommandService deviceCommandService) {
        this.deviceCommandService = deviceCommandService;
    }

    @PostMapping("/")
    public ResponseEntity<?> sendCommand(@RequestBody Map<String, Object> body) {
        logger.debug("POST /api/consumer/commands - body: {}", body);

        String deviceId = (String) body.get("device_id");
        String command = (String) body.get("command");

        if (deviceId == null || deviceId.isBlank() || command == null || command.isBlank()) {
            logger.debug("POST /api/consumer/commands - rejected: missing device_id or command");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "'device_id' and 'command' are required"));
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) body.get("params");
            deviceCommandService.sendCommand(deviceId, command, params);
        } catch (Exception e) {
            logger.error("POST /api/consumer/commands - failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "failed to write command to Firestore"));
        }

        logger.debug("POST /api/consumer/commands - command '{}' sent to device '{}'", command, deviceId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("msg", "command sent"));
    }

}