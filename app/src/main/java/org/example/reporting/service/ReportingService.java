package org.example.reporting.service;

import lombok.NonNull;
import org.example.consumer.model.Producer;
import org.example.consumer.model.Stream;
import org.example.consumer.model.TimeSeriesRecord;
import org.example.consumer.stream.utils.Translators;
import org.example.libb3project.dto.ProducerDTO;
import org.example.libb3project.dto.StreamDTO;
import org.example.libb3project.dto.TimeSeriesRecordDTO;
import org.example.consumer.repository.ProducerRepository;
import org.example.consumer.repository.StreamRepository;
import org.example.consumer.repository.TimeseriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReportingService {

    private static final Logger logger = LoggerFactory.getLogger(ReportingService.class);

    private final StreamRepository streamRepository;
    private final ProducerRepository producerRepository;
    private final TimeseriesRepository timeseriesRepository;

    @Autowired
    public ReportingService(
            StreamRepository streamRepository,
            ProducerRepository producerRepository,
            TimeseriesRepository timeseriesRepository,
            Translators translators
    ) {
        this.streamRepository = streamRepository;
        this.producerRepository = producerRepository;
        this.timeseriesRepository = timeseriesRepository;
    }

    public List<StreamDTO> getAllStreams() {
        logger.debug("getAllStreams - querying repository");
        List<StreamDTO> streams = streamRepository.findAll().stream()
                .map(Stream::toDTO)
                .toList();
        logger.debug("getAllStreams - returning {} stream(s)", streams.size());
        return streams;
    }

    public List<StreamDTO> getStreamHierarchy() {
        logger.debug("getStreamHierarchy - querying repository");
        List<Stream> allStreams = streamRepository.findAll();
        logger.debug("getStreamHierarchy - building DTO map from {} stream(s)", allStreams.size());
        List<StreamDTO> roots = Translators.hierarchyFromStreams(allStreams);
        logger.debug("getStreamHierarchy - returning {} root stream(s)", roots.size());
        return roots;
    }

    public List<ProducerDTO> getAllProducers() {
        logger.debug("getAllProducers - querying repository");
        List<ProducerDTO> producers = producerRepository.findAll().stream()
                .map(Producer::toDTO)
                .toList();
        logger.debug("getAllProducers - returning {} producer(s)", producers.size());
        return producers;
    }

    public List<ProducerDTO> getAllProducersForStream(String streamName) {
        logger.debug("getAllProducersForStream - querying for stream '{}'", streamName);
        List<ProducerDTO> producers = producerRepository.findByStreamName(streamName).stream()
                .map(Producer::toDTO)
                .toList();
        logger.debug("getAllProducersForStream - returning {} producer(s) for stream '{}'", producers.size(), streamName);
        return producers;
    }

    public List<ProducerDTO> getProducersByStreamId(UUID streamId) {
        logger.debug("getProducersByStreamId - querying for streamId={}", streamId);
        List<ProducerDTO> producers = producerRepository.findByStreamUuid(streamId).stream()
                .map(Producer::toDTO)
                .toList();
        logger.debug("getProducersByStreamId - returning {} producer(s) for streamId={}", producers.size(), streamId);
        return producers;
    }

    public Optional<ProducerDTO> getProducerByName(String name) {
        logger.debug("getProducerByName - querying for name='{}'", name);
        return Optional.ofNullable(producerRepository.findByName(name)).map(Producer::toDTO);
    }

    public Optional<StreamDTO> getStreamByName(String name) {
        logger.debug("getStreamByName - querying for name='{}'", name);
        return Optional.ofNullable(streamRepository.findByName(name)).map(Stream::toDTO);
    }

    public String getStreamNameById(UUID streamId) {
        logger.debug("getStreamNameById - querying for streamId={}", streamId);
        return streamRepository.findById(streamId)
                .map(Stream::getName)
                .orElse(null);
    }

    public List<TimeSeriesRecordDTO> getRecordsByStreamId(UUID streamId) {
        logger.debug("getRecordsByStreamId - querying for streamId={}", streamId);
        List<TimeSeriesRecordDTO> records = timeseriesRepository.findByProducerStreamUuid(streamId).stream()
                .map(TimeSeriesRecord::toDTO)
                .toList();
        logger.debug("getRecordsByStreamId - returning {} record(s) for streamId={}", records.size(), streamId);
        return records;
    }

}
