package org.cotc.exception;

public class GroupNotFoundException extends Exception {

    private final String path;

    public GroupNotFoundException(String groupName) {
        super("Group does not exist: " + groupName);
        this.path = groupName;
    }

    public String getPath() {
        return path;
    }
}
