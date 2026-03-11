package org.cotc.service;

import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class DeviceCommandService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceCommandService.class);
    private final Firestore firestore;

    @Autowired
    public DeviceCommandService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void sendCommand(String deviceId, String command, Map<String, Object> params) throws Exception {
        logger.debug("sendCommand called with deviceId={}, command={}, params={}", deviceId, command, params);

        Map<String, Object> commandDoc = new HashMap<>();
        commandDoc.put("device_id", deviceId);
        commandDoc.put("command", command);
        commandDoc.put("params", params != null ? params : Map.of());
        commandDoc.put("processed", false);
        commandDoc.put("created_at", System.currentTimeMillis());

        firestore.collection("device_commands").add(commandDoc).get(); // block to ensure command is written before returning
        logger.info("Command '{}' sent to device '{}'", command, deviceId);

    }
}