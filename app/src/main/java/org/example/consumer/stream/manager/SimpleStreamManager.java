package org.example.consumer.stream.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.example.consumer.model.TimeSeriesRecord;
import org.example.consumer.model.dto.TimeSeriesMessageDTO;
import org.example.consumer.repository.StreamRepository;
import org.example.consumer.repository.TimeseriesRepository;
import org.example.consumer.stream.exception.InvalidSubscriptionTreePathFormatException;
import org.example.consumer.stream.exception.SubscriptionAlreadyExistsException;
import org.example.consumer.stream.exception.TreePathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
class SimpleStreamManager implements StreamManager {
    private static final Logger logger = LoggerFactory.getLogger(SimpleStreamManager.class);

    private final StreamRepository streamRepository;
    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final TimeseriesRepository timeseriesRepository;

    @Autowired
    SimpleStreamManager(StreamRepository streamRepository, NatsConnectionSingleton natsConnectionSingleton, ObjectMapper objectMapper, TimeseriesRepository timeseriesRepository) {
        this.streamRepository = streamRepository;
        this.natsConnection = natsConnectionSingleton.getConnection();
        this.objectMapper = objectMapper;
        this.timeseriesRepository = timeseriesRepository;
    }

    @Override
    public void createStream(String name, String parent) throws TreePathNotFoundException, InvalidSubscriptionTreePathFormatException, SubscriptionAlreadyExistsException {
        logger.debug("createStream - name='{}'", name);

        // validate that it is in the format '<stream_name>' or '<stream_name>.<child_stream_name>'
        InvalidSubscriptionTreePathFormatException.validate(name);

        // any parent streams must exist
        if (!isRootStreamName(name)) {
            String parentStreamName = getParentStreamName(name);
            logger.debug("createStream - checking parent stream exists: '{}'", parentStreamName);
            if (!streamRepository.streamExists(parentStreamName)) {
                logger.debug("createStream - parent stream '{}' not found, throwing TreePathNotFoundException", parentStreamName);
                throw new TreePathNotFoundException(parentStreamName);
            }
        }

        logger.debug("recording new stream name in database: name={}", name);
        streamRepository.newStream(name);
        logger.debug("initializing dispatcher for stream: {}", name);
        initDispatcher(name);
        logger.info("createStream - stream '{}' created successfully", name);
    }

    @Override
    public void restoreStream(String name, String parent) throws TreePathNotFoundException {
        initDispatcher(name);
    }

    @Override
    public void deleteStream(String name) {
        logger.debug("deleteStream - name='{}'", name);
        if (!streamRepository.streamExists(name)) {
            logger.debug("deleteStream - stream '{}' not found, nothing to delete", name);
            return;
        }
        streamRepository.deleteAllDescendants(name);
        logger.debug("deleteStream - deleted all descendants of '{}'", name);
        streamRepository.deleteByName(name);
        logger.debug("deleteStream - stream '{}' deleted successfully", name);
    }

    @Override
    public List<String> getAllStreamNames() {
        logger.debug("getAllStreamNames - fetching all stream names");
        List<String> names = streamRepository.findAll().stream()
                .map(stream -> stream.getName())
                .toList();
        logger.debug("getAllStreamNames - returning {} stream names", names.size());
        return names;
    }

    @Override
    public List<String> getChildStreams(String parentPath) throws TreePathNotFoundException, InvalidSubscriptionTreePathFormatException {
        logger.debug("getChildStreams - parentPath='{}'", parentPath);
        if (!streamRepository.streamExists(parentPath)) {
            logger.debug("getChildStreams - stream '{}' not found, throwing TreePathNotFoundException", parentPath);
            throw new TreePathNotFoundException(parentPath);
        }
        List<String> children = streamRepository.getChildren(parentPath).stream()
                .map(stream -> stream.getName())
                .toList();
        logger.debug("getChildStreams - found {} children for '{}'", children.size(), parentPath);
        return children;
    }

    void initDispatcher(String streamName) {
        logger.debug("initDispatcher - initializing NATS dispatcher for stream '{}'", streamName);
        Dispatcher dispatcher = natsConnection.createDispatcher(message -> {
            try {
                TimeSeriesMessageDTO dto = objectMapper.readValue(message.getData(), TimeSeriesMessageDTO.class);
                logger.debug("initDispatcher - stream '{}' received message {}", streamName, dto);
                Instant readTime = Instant.ofEpochSecond(dto.getReadTime());
                for (Map.Entry<String, Double> entry : dto.getValues().entrySet()) {
                    TimeSeriesRecord record = new TimeSeriesRecord(null, entry.getKey(), entry.getValue().floatValue(), dto.getProducer(), readTime);
                    timeseriesRepository.save(record);
                }
            } catch (Exception e) {
                logger.debug("initDispatcher - stream '{}' failed to parse message: {}", streamName, e.getMessage());
            }
        });
        dispatcher.subscribe(streamName);
        logger.debug("initDispatcher - NATS dispatcher initialized for stream '{}'", streamName);
    }

    String getParentStreamName(String name) {
        return name.substring(0, name.indexOf('.'));
    }

    boolean isRootStreamName(String name) {
        return !name.contains(".");
    }

    @Override
    public AutoCloseable subscribeToStream(String streamName, Consumer<byte[]> handler) {
        logger.debug("subscribeToStream - creating live NATS subscription for stream '{}'", streamName);
        Dispatcher dispatcher = natsConnection.createDispatcher(msg -> handler.accept(msg.getData()));
        dispatcher.subscribe(streamName);
        logger.debug("subscribeToStream - live subscription active for stream '{}'", streamName);
        return () -> dispatcher.unsubscribe(streamName);
    }
}
