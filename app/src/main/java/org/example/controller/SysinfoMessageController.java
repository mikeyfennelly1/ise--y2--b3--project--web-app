package org.example.controller;

import org.example.model.MacAddressMetadata;
import org.example.repository.SysinfoMessageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sysinfo")
public class SysinfoMessageController {

    private final SysinfoMessageRepository sysinfoMessageRepository;

    public SysinfoMessageController(SysinfoMessageRepository sysinfoMessageRepository) {
        this.sysinfoMessageRepository = sysinfoMessageRepository;
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
}
