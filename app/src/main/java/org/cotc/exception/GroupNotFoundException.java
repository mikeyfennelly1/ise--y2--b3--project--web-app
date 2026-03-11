package org.cotc.exception;

public class GroupNotFoundException extends Exception {

    private final String path;

    public GroupNotFoundException(String path) {
        super("Subscription tree path not found: " + path);
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
