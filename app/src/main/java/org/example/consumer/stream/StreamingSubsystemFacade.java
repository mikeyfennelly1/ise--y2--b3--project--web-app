package org.example.consumer.stream;

import org.example.consumer.stream.exception.InvalidSubscriptionTreePathFormatException;
import org.example.consumer.stream.exception.SubscriptionAlreadyExistsException;
import org.example.consumer.stream.exception.TreePathNotFoundException;
import org.example.consumer.stream.manager.StreamManager;
import org.example.consumer.stream.manager.StreamManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StreamingSubsystemFacade {
    private static final Logger logger = LoggerFactory.getLogger(StreamingSubsystemFacade.class);

    private final StreamManager manager;

    @Autowired
    public StreamingSubsystemFacade(StreamManagerFactory factory) {
        this.manager = factory.getManager("simple");
    }

    public void createStream(String name, String parentFullName) throws TreePathNotFoundException, InvalidSubscriptionTreePathFormatException, SubscriptionAlreadyExistsException {
        logger.debug("createStream - name='{}' parent='{}'", name, parentFullName);
        manager.createStream(name, parentFullName);
    }

    public void removeStream(String subscriptionName) {
        manager.deleteStream(subscriptionName);
    }

    public List<String> getAllStreamNames() {
        return manager.getAllStreamNames();
    }

    public List<String> getChildStreams(String parentPath) throws TreePathNotFoundException, InvalidSubscriptionTreePathFormatException {
        logger.debug("getChildSubscriptions - parentPath='{}'", parentPath);
        List<String> children = manager.getChildStreams(parentPath);
        logger.debug("getChildStreams - found {} children", children.size());
        return children;
    }
}
