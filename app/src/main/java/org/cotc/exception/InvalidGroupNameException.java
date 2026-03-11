package org.cotc.exception;

public class InvalidGroupNameException extends Exception {

    private final String invalidPath;

    public InvalidGroupNameException(String invalidPath) {
        super("Invalid subscription tree path: \"" + invalidPath + "\". "
                + "Paths must be non-empty tokens separated by dots (e.g. \"device\" or \"device.sysinfo\"). "
                + "Null, blank, leading/trailing dots, and consecutive dots are not allowed.");
        this.invalidPath = invalidPath;
    }

    public String getInvalidPath() {
        return invalidPath;
    }

    /**
     * Validates that {@code path} is either a single subject name (e.g. {@code "device"})
     * or n subject names separated by a dot (e.g. {@code "device.sysinfo"}).
     * Each token must be non-empty. Null, blank, leading/trailing dots,
     * and consecutive dots are not allowed.
     *
     * @throws InvalidGroupNameException if the path is invalid.
     */
    public static void validate(String path) throws InvalidGroupNameException {
        if (path == null || path.isBlank()) {
            throw new InvalidGroupNameException(path);
        }
        String[] parts = path.split("\\.", -1);
        for (String part : parts) {
            if (part.isEmpty()) {
                throw new InvalidGroupNameException(path);
            }
        }
    }
}
