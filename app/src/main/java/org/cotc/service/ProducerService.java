package org.cotc.service;

import org.cotc.libcotc.dto.ProducerDTO;
import org.cotc.model.Producer;
import org.cotc.repository.ProducerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProducerService {
    private static final Logger logger = LoggerFactory.getLogger(ProducerService.class);
    private final ProducerRepository producerRepository;

    @Autowired
    ProducerService(ProducerRepository producerRepository) {
        this.producerRepository = producerRepository;
    }

    public List<ProducerDTO> getAllProducerDtos() {
        logger.debug("getAllProducers - querying repository");
        List<ProducerDTO> producers = producerRepository.findAll().stream()
                .map(Producer::toDTO)
                .toList();
        logger.debug("getAllProducers - returning {} producer(s)", producers.size());
        return producers;
    }

    public List<ProducerDTO> getAllProducerDtosForGroup(String groupName) {
        logger.debug("getAllProducersForStream - querying for stream '{}'", groupName);
        List<ProducerDTO> producers = producerRepository.findByGroupName(groupName).stream()
                .map(Producer::toDTO)
                .toList();
        logger.debug("getAllProducersForStream - returning {} producer(s) for stream '{}'", producers.size(), groupName);
        return producers;
    }

    public ProducerDTO createNewProducer(Producer producer) {
        producerRepository.save(producer);
        return producer.toDTO();
    }

    public List<ProducerDTO> getProducerDtosByGroupUuid(UUID streamId) {
        logger.debug("getProducersByGroupId - querying for streamId={}", streamId);
        List<ProducerDTO> producers = producerRepository.findByGroupUuid(streamId).stream()
                .map(Producer::toDTO)
                .toList();
        logger.debug("getProducersByGroupId - returning {} producer(s) for streamId={}", producers.size(), streamId);
        return producers;
    }

    public Optional<ProducerDTO> getProducerDtoByName(String name) {
        logger.debug("getProducerByName - querying for name='{}'", name);
        return Optional.ofNullable(producerRepository.findByName(name)).map(Producer::toDTO);
    }

    public Producer getProducerByName(String name) {
        return producerRepository.findByName(name);
    }
}
