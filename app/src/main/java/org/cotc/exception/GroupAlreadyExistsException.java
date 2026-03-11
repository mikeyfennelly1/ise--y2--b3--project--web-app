package org.cotc.exception;

public class GroupAlreadyExistsException extends Exception {

    private final String path;

    public GroupAlreadyExistsException(String path) {
        super("Subscription already exists at path: \"" + path + "\".");
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
