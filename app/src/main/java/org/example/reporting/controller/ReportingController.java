package org.example.reporting.controller;

import org.example.consumer.repository.TimeseriesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reporting")
public class ReportingController {

    private final TimeseriesRepository sysinfoMessageRepository;

    @Autowired
    public ReportingController(
            TimeseriesRepository sysinfoMessageRepository
    ) {
        this.sysinfoMessageRepository = sysinfoMessageRepository;
    }
}
