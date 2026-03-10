package org.example.consumer.stream.manager;

import org.example.consumer.repository.StreamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


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


    public StreamManager getManager(String managerName) {
        switch (managerName) {
            case "simple":
                return this.simple;
            default:
                throw new IllegalArgumentException("manager of provided type does not exist: " + managerName);
        }
    }
}
