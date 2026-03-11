package org.example.stream.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import jakarta.annotation.PostConstruct;
import org.example.model.Stream;
import org.example.repository.TimeseriesRepository;
import org.example.stream.exception.StreamAlreadyExistsException;
import org.example.repository.StreamRepository;
import org.example.stream.exception.InvalidStreamNameException;
import org.example.stream.exception.StreamNotFoundException;
import org.example.libb3project.dto.TimeSeriesMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.example.stream.utils.SubscriptionNameUtils.getParentStreamName;
import static org.example.stream.utils.SubscriptionNameUtils.isRootStreamName;

@Component
class DurableStreamManager implements StreamManager {
    private static final Logger logger = LoggerFactory.getLogger(DurableStreamManager.class);
    private final StreamRepository streamRepository;
    private final Map<String, TimeSeriesStreamHandler> managedStreams = new HashMap<>();
    private final Connection natsConnection;
    private final TimeSeriesStreamHandler dispatchMessageHandler;
    private final ObjectMapper mapper;
    private final TimeseriesRepository timeseriesRepository;

    @Autowired
    DurableStreamManager(
            ObjectMapper mapper,
            StreamRepository streamRepository,
            NatsConnectionSingleton natsConnSingleton,
            TimeseriesRepository timeseriesRepository
    ) {
        this.streamRepository = streamRepository;
        this.natsConnection = natsConnSingleton.getConnection();
        this.dispatchMessageHandler = new TimeSeriesStreamHandler();
        this.mapper = mapper;
        this.timeseriesRepository = timeseriesRepository;
    }

    @PostConstruct
    private void init() throws BeanCreationException, InterruptedException {
        List<Stream> streams = streamRepository.findAll();
        logger.debug("init - found {} stream(s) in database to restore", streams.size());
        if (streams.isEmpty()) {
            logger.debug("init - no streams to restore, skipping");
            return;
        }
        restoreAllStreams(streams);
    }

    private void restoreAllStreams(List<Stream> streams) throws InterruptedException {
        int n = streams.size();
        logger.debug("init - submitting {} restore task(s) to virtual thread executor", n);
        CountDownLatch latch = new CountDownLatch(n);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Stream stream : streams) {
                executor.submit(() -> {
                    this.newManagedStream(stream.getName());
                    latch.countDown();
                });
            }
        }
        logger.debug("init - waiting for all restore tasks to complete");
        latch.await();
        logger.debug("init - all {} stream(s) restored successfully", n);
    }

    @Override
    public void createStream(String name, String parent) throws InvalidStreamNameException, StreamNotFoundException, StreamAlreadyExistsException {
        logger.debug("createStream - name='{}'", name);
        streamIsCreatable(name);
        createAndManageStream(name);
    }

    @Override
    public void createAndManageStream(String name) {
        newManagedStream(name);
        streamRepository.newStream(name);
    }

    private void newManagedStream(String name) {

    }

    private void streamIsCreatable(String name) throws InvalidStreamNameException, StreamAlreadyExistsException, StreamNotFoundException {
        // validate that it is in the format '<stream_name>' or '<stream_name>.<child_stream_name>'
        InvalidStreamNameException.validate(name);
        if (streamAlreadyExists(name)) {
            throw new StreamAlreadyExistsException(name);
        }
        // any parent streams must exist
        if (!isRootStreamName(name)) {
            String parentStreamName = getParentStreamName(name);
            logger.debug("createStream - checking parent stream exists: '{}'", parentStreamName);
            if (!streamRepository.streamExists(parentStreamName)) {
                logger.debug("createStream - parent stream '{}' not found, throwing TreePathNotFoundException", parentStreamName);
                throw new StreamNotFoundException(parentStreamName);
            }
        }
    }

    @Override
    public void deleteStream(String name) throws StreamNotFoundException {
        logger.debug("deleteStream - name='{}'", name);
        if (!streamRepository.streamExists(name)) {
            logger.debug("deleteStream - stream '{}' not found, throwing TreePathNotFoundException", name);
            throw new StreamNotFoundException(name);
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
    public List<String> getChildStreams(String parentPath) throws InvalidStreamNameException, StreamNotFoundException {
        logger.debug("getChildStreams - parentPath='{}'", parentPath);
        if (!streamRepository.streamExists(parentPath)) {
            logger.debug("getChildStreams - stream '{}' not found, throwing TreePathNotFoundException", parentPath);
            throw new StreamNotFoundException(parentPath);
        }
        List<String> children = streamRepository.getChildren(parentPath).stream()
                .map(stream -> stream.getName())
                .toList();
        logger.debug("getChildStreams - found {} children for '{}'", children.size(), parentPath);
        return children;
    }

    @Override
    public Flux<TimeSeriesMessageDTO> getStreamSSESink(String streamName) {
        return this.managedStreams.get(streamName).getFlux();
    }

    @Override
    public boolean streamAlreadyExists(String streamName) {
        return this.managedStreams.containsKey(streamName);
    }
}
