package org.example.consumer.subscriber;

import org.example.consumer.subscriber.exception.InvalidSubscriptionTreePathFormatException;
import org.example.consumer.subscriber.exception.SubscriptionAlreadyExistsException;
import org.example.consumer.subscriber.exception.TreePathNotFoundException;
import org.example.consumer.subscriber.manager.SubscriptionManager;
import org.example.consumer.subscriber.manager.tree.SubscriptionNode;
import org.example.consumer.subscriber.manager.tree.SubscriptionTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SubscriptionSubsystemFacade implements SubscriptionManager {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionSubsystemFacade.class);

    private final SubscriptionTree subscriptionTree;

    @Autowired
    public SubscriptionSubsystemFacade(SubscriptionTree subscriptionTree) {
        this.subscriptionTree = subscriptionTree;
    }

    @Override public void createSubscription(String name, String parentFullName) throws TreePathNotFoundException, InvalidSubscriptionTreePathFormatException, SubscriptionAlreadyExistsException {
        logger.debug("newSubscription - name='{}' parent='{}'", name, parentFullName);
        subscriptionTree.createSubscription(name, parentFullName);
    }

    @Override public void deleteSubscription(String subscriptionName) {

    }

    @Override public List<String> readAllSubscriptions() {
        return subscriptionTree.readAllSubscriptions();
    }

    public List<SubscriptionNode> getChildSubscriptions(String parentPath) throws TreePathNotFoundException, InvalidSubscriptionTreePathFormatException {
        logger.debug("getChildSubscriptions - parentPath='{}'", parentPath);
        List<SubscriptionNode> children = subscriptionTree.getChildrenOfNode(parentPath);
        logger.debug("getChildSubscriptions - found {} children", children.size());
        return children;
    }
}
