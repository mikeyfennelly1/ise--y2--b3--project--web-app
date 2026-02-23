package org.example.reporting.config.controller;

import org.example.consumer.model.device.Device;
import org.example.consumer.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceRepository deviceRepository;

    @Autowired
    public DeviceController(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @GetMapping
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    @GetMapping("/{macAddress}")
    public ResponseEntity<Device> getDevice(@PathVariable String macAddress) {
        return deviceRepository.findById(macAddress)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Device registerDevice(@RequestBody Device device) {
        return deviceRepository.save(device);
    }

    @PutMapping("/{macAddress}")
    public ResponseEntity<Device> updateDevice(@PathVariable String macAddress, @RequestBody Device updated) {
        if (!deviceRepository.existsById(macAddress)) {
            return ResponseEntity.notFound().build();
        }
        updated.setMacAddress(macAddress);
        return ResponseEntity.ok(deviceRepository.save(updated));
    }
}
