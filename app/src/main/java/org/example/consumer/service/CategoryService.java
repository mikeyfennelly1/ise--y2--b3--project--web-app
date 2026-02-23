package org.example.consumer.service;

import org.example.consumer.model.SourceCategory;
import org.example.consumer.model.device.SysinfoMessage;
import org.example.consumer.repository.TimeseriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final TimeseriesRepository sysinfoMessageRepository;

    @Autowired
    public CategoryService(TimeseriesRepository sysinfoMessageRepository) {
        this.sysinfoMessageRepository = sysinfoMessageRepository;
    }

    public List<String> getDeviceTypes() {
        return List.of("device");
    }

    public List<String> getSourcesByCategory(SourceCategory category, String subtype) {
        if (category == SourceCategory.device) {
            return sysinfoMessageRepository.findDistinctMacAddresses();
        }
        return List.of();
    }

    public List<SysinfoMessage> getSysinfoReport(String deviceId, Long startDate, Long endDate) {
        return sysinfoMessageRepository.findByMacAddressAndReadTimeBetween(deviceId, startDate, endDate);
    }
}
