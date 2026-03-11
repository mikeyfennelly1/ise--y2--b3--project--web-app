package org.cotc.exception;

public class GroupAlreadyExistsException extends Exception {

    private final String path;

    public GroupAlreadyExistsException(String path) {
        super("Group already exists: \"" + path + "\".");
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
