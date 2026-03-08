package org.example.reporting.service;

import org.example.consumer.model.Producer;
import org.example.consumer.model.Stream;
import org.example.consumer.model.TimeSeriesRecord;
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
    public ReportingService(StreamRepository streamRepository, ProducerRepository producerRepository, TimeseriesRepository timeseriesRepository) {
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

        // Build name -> StreamDTO map (children must be mutable for tree assembly)
        Map<String, StreamDTO> dtoMap = new LinkedHashMap<>();
        for (Stream stream : allStreams) {
            List<ProducerDTO> producers = stream.getProducers().stream()
                    .map(Producer::toDTO)
                    .toList();
            logger.trace("getStreamHierarchy - mapped stream '{}' with {} producer(s)", stream.getName(), producers.size());
            dtoMap.put(stream.getName(), stream.toDTO());
        }

        // Attach each node to its parent; collect nodes with no parent as roots
        List<StreamDTO> roots = new ArrayList<>();
        for (Map.Entry<String, StreamDTO> entry : dtoMap.entrySet()) {
            String name = entry.getKey();
            StreamDTO dto = entry.getValue();
            int lastDot = name.lastIndexOf('.');
            if (lastDot == -1) {
                logger.trace("getStreamHierarchy - '{}' is a root stream", name);
                roots.add(dto);
            } else {
                String parentName = name.substring(0, lastDot);
                StreamDTO parent = dtoMap.get(parentName);
                if (parent != null) {
                    logger.trace("getStreamHierarchy - attaching '{}' as child of '{}'", name, parentName);
                    parent.getChildren().add(dto);
                } else {
                    logger.trace("getStreamHierarchy - parent '{}' not found for '{}', treating as root", parentName, name);
                    roots.add(dto);
                }
            }
        }

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
