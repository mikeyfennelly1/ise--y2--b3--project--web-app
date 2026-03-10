package org.example.consumer.stream.exception;

public class StreamNotFoundException extends Exception {

    private final String path;

    public StreamNotFoundException(String path) {
        super("Subscription tree path not found: " + path);
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
