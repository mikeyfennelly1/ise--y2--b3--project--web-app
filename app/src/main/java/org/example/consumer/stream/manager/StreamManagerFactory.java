package org.example.consumer.stream.manager;

import jakarta.annotation.PostConstruct;
import org.example.consumer.model.Stream;
import org.example.consumer.repository.StreamRepository;
import org.example.consumer.stream.exception.StreamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

@Component
public class StreamManagerFactory {
    private static final Logger logger = LoggerFactory.getLogger(StreamManagerFactory.class);

    private final StreamManager simple;
    private final StreamRepository streamRepository;

    @Autowired
    StreamManagerFactory(
            SimpleStreamManager simple,
            StreamRepository streamRepository
    ) {
        this.simple = simple;
        this.streamRepository = streamRepository;
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
                    restoreStream(stream, latch);
                });
            }
        }
        logger.debug("init - waiting for all restore tasks to complete");
        latch.await();
        logger.debug("init - all {} stream(s) restored successfully", n);
    }

    private void restoreStream(Stream stream, CountDownLatch latch) {
        logger.debug("init - restoring stream '{}'", stream.getName());
        try {
            try {
                this.simple.restoreStream(stream.getName(), null);
                logger.debug("init - stream '{}' restored successfully", stream.getName());
            } catch (StreamNotFoundException e) {
                logger.error("init - failed to restore stream '{}' (cause={}): {}", stream.getName(), e.getCause(), e.getMessage());
            }
        } finally {
            latch.countDown();
            logger.debug("init - latch count remaining: {}", latch.getCount());
        }
    }

    public StreamManager getManager(String managerName) {
        switch (managerName) {
            case "simple":
                return this.simple;
            default:
                throw new IllegalArgumentException("manager of provided type does not exist: " + managerName);
        }
    }
}
