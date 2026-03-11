package org.example.stream.exception;

public class InvalidStreamNameException extends Exception {

    private final String invalidPath;

    public InvalidStreamNameException(String invalidPath) {
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
     * @throws InvalidStreamNameException if the path is invalid.
     */
    public static void validate(String path) throws InvalidStreamNameException {
        if (path == null || path.isBlank()) {
            throw new InvalidStreamNameException(path);
        }
        String[] parts = path.split("\\.", -1);
        for (String part : parts) {
            if (part.isEmpty()) {
                throw new InvalidStreamNameException(path);
            }
        }
    }
}
