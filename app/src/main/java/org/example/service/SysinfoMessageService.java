package org.example.service;

import org.example.model.SysinfoMessage;
import org.example.repository.SysinfoMessageRepository;
import org.springframework.stereotype.Service;

@Service
public class SysinfoMessageService {

    private final SysinfoMessageRepository repository;

    public SysinfoMessageService(SysinfoMessageRepository repository) {
        this.repository = repository;
    }

    public SysinfoMessage save(SysinfoMessage message) {
        return repository.save(message);
    }
}
