package org.example.reporting.service;

import org.example.consumer.model.Producer;
import org.example.consumer.model.Stream;
import org.example.consumer.model.TimeSeriesRecord;
import org.example.consumer.model.dto.ProducerDTO;
import org.example.consumer.model.dto.SourceDTO;
import org.example.consumer.model.dto.StreamDTO;
import org.example.consumer.model.dto.TimeSeriesRecordDTO;
import org.example.consumer.repository.ProducerRepository;
import org.example.consumer.repository.StreamRepository;
import org.example.consumer.repository.TimeseriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                .map(this::toDTO)
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
            List<SourceDTO> producers = stream.getProducers().stream()
                    .map(p -> new SourceDTO(p.getId(), p.getSourceName()))
                    .toList();
            logger.trace("getStreamHierarchy - mapped stream '{}' with {} producer(s)", stream.getName(), producers.size());
            dtoMap.put(stream.getName(), new StreamDTO(stream.getId(), stream.getName(), new ArrayList<>(), producers));
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
                .map(this::toDTO)
                .toList();
        logger.debug("getAllProducers - returning {} producer(s)", producers.size());
        return producers;
    }

    public List<ProducerDTO> getAllProducersForStream(String streamName) {
        logger.debug("getAllProducersForStream - querying for stream '{}'", streamName);
        List<ProducerDTO> producers = producerRepository.findByStreamName(streamName).stream()
                .map(this::toDTO)
                .toList();
        logger.debug("getAllProducersForStream - returning {} producer(s) for stream '{}'", producers.size(), streamName);
        return producers;
    }

    public List<ProducerDTO> getProducersByStreamId(Long streamId) {
        logger.debug("getProducersByStreamId - querying for streamId={}", streamId);
        List<ProducerDTO> producers = producerRepository.findByStreamId(streamId).stream()
                .map(this::toDTO)
                .toList();
        logger.debug("getProducersByStreamId - returning {} producer(s) for streamId={}", producers.size(), streamId);
        return producers;
    }

    public List<TimeSeriesRecordDTO> getRecordsByStreamId(Long streamId) {
        logger.debug("getRecordsByStreamId - querying for streamId={}", streamId);
        List<TimeSeriesRecordDTO> records = timeseriesRepository.findByProducerStreamId(streamId).stream()
                .map(this::toDTO)
                .toList();
        logger.debug("getRecordsByStreamId - returning {} record(s) for streamId={}", records.size(), streamId);
        return records;
    }

    private StreamDTO toDTO(Stream stream) {
        List<SourceDTO> producers = stream.getProducers().stream()
                .map(p -> new SourceDTO(p.getId(), p.getSourceName()))
                .toList();
        return new StreamDTO(stream.getId(), stream.getName(), List.of(), producers);
    }

    private TimeSeriesRecordDTO toDTO(TimeSeriesRecord record) {
        return new TimeSeriesRecordDTO(record.getId(), record.getKey(), record.getValue(), record.getProducer().getSourceName(), record.getReadTime());
    }

    private ProducerDTO toDTO(Producer producer) {
        return new ProducerDTO(producer.getId(), producer.getSourceName(), producer.getStream().getName());
    }
}
