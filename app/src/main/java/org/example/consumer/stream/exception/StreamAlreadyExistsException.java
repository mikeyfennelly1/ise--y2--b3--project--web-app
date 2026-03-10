package org.example.consumer.stream.exception;

public class StreamAlreadyExistsException extends Exception {

    private final String path;

    public StreamAlreadyExistsException(String path) {
        super("stream already exists at path: \"" + path + "\".");
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
