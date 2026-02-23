package org.example.controller;

import org.example.model.source.device.MacAddressMetadata;
import org.example.model.source.device.SysinfoMessage;
import org.example.repository.SysinfoMessageRepository;
import org.example.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sysinfo")
public class SysinfoController {

    private final SysinfoMessageRepository sysinfoMessageRepository;
    private final CategoryService categoryService;

    @Autowired
    public SysinfoController(
            SysinfoMessageRepository sysinfoMessageRepository,
            CategoryService categoryService
    ) {
        this.sysinfoMessageRepository = sysinfoMessageRepository;
        this.categoryService = categoryService;
    }

    @GetMapping("/mac-addresses")
    public List<String> getDistinctMacAddresses() {
        return sysinfoMessageRepository.findDistinctMacAddresses();
    }

    @GetMapping("/devices")
    public ResponseEntity<?> getSysinfoMessagesByMacAddress(@RequestParam(name = "mac_addr", required = false) String macAddress) {
        if (macAddress == null) {
            // Requirement: return a list of all unique MAC addresses
            List<String> macAddresses = sysinfoMessageRepository.findDistinctMacAddresses();
            return ResponseEntity.ok(macAddresses);
        } else {
            // Requirement: return metadata for a specific MAC, or 404 if not found
            if (sysinfoMessageRepository.existsByMacAddress(macAddress)) {
                return ResponseEntity.ok(new MacAddressMetadata(macAddress));
            } else {
                return ResponseEntity.notFound().build();
            }
        }
    }



    @GetMapping("/")
    public List<SysinfoMessage> getSysinfoReport(
            @RequestParam("device_id") String deviceId,
            @RequestParam("start_date") Long startDate,
            @RequestParam("end_date") Long endDate) {
        return categoryService.getSysinfoReport(deviceId, startDate, endDate);
    }
}
