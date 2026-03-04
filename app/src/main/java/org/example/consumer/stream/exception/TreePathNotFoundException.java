package org.example.consumer.stream.exception;

public class TreePathNotFoundException extends Exception {

    private final String path;

    public TreePathNotFoundException(String path) {
        super("Subscription tree path not found: " + path);
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
