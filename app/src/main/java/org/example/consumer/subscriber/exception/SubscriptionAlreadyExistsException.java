package org.example.consumer.subscriber.exception;

public class SubscriptionAlreadyExistsException extends Exception {

    private final String path;

    public SubscriptionAlreadyExistsException(String path) {
        super("Subscription already exists at path: \"" + path + "\".");
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
