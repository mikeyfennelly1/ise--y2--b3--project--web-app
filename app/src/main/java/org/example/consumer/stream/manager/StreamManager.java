package org.example.consumer.stream.manager;

import org.example.consumer.stream.exception.InvalidSubscriptionTreePathFormatException;
import org.example.consumer.stream.exception.SubscriptionAlreadyExistsException;
import org.example.consumer.stream.exception.TreePathNotFoundException;

import java.util.List;
import java.util.function.Consumer;

public interface StreamManager {
    void createStream(String name, String parent) throws TreePathNotFoundException, InvalidSubscriptionTreePathFormatException, SubscriptionAlreadyExistsException;
    void restoreStream(String name, String parent) throws TreePathNotFoundException;
    void deleteStream(String subscriptionName);
    List<String> getAllStreamNames();
    List<String> getChildStreams(String parentPath) throws TreePathNotFoundException, InvalidSubscriptionTreePathFormatException;
    AutoCloseable subscribeToStream(String streamName, Consumer<byte[]> handler);
}
