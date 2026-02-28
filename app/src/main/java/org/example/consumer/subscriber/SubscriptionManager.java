package org.example.consumer.subscriber;

import java.util.List;

public interface SubscriptionManager {
    void newSubscription(String name, String parent) throws TreePathNotFoundException, InvalidSubscriptionTreePathFormatException, SubscriptionAlreadyExistsException;
    void removeSubscription(String subscriptionName);
    List<String> getActiveSubscriptions();
}
