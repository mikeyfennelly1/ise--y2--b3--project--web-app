package org.example.consumer.subscriber;

import java.util.List;

class SubscriptionNameUtils {
    public Builder builder(String rootName) {
        return new Builder(rootName);
    }

    static class Builder {
        private static StringBuilder fullName;

        private Builder(String rootName) {
            fullName.append(rootName);
        }

        public void addName(String name) throws IllegalArgumentException {
            if (isValidSubjectFormat(name)) {
                fullName.append(".").append(name);
            } else {
                throw new IllegalArgumentException("invalid subject format: " + name);
            }
        }

        public String build() {
            return fullName.toString();
        }
    }

    /**
     * Splits a subject name by full stop into an ordered list of its tokens.
     * e.g. "device.sysinfo" → ["device", "sysinfo"], "device" → ["device"]
     *
     * @throws IllegalArgumentException if the subject is not a valid format.
     */
    static List<String> splitSubject(String subject) {
        if (!isValidSubjectFormat(subject)) {
            throw new IllegalArgumentException("invalid subject format: " + subject);
        }
        return List.of(subject.split("\\."));
    }

    /**
     * Returns true if the subject is a single non-empty token or a sequence of
     * non-empty tokens separated by full stops, e.g. "device" or "device.sysinfo".
     * Rejects null, blank strings, leading/trailing dots, and consecutive dots.
     */
    static boolean isValidSubjectFormat(String subject) {
        if (subject == null || subject.isBlank()) return false;
        String[] parts = subject.split("\\.", -1);
        for (String part : parts) {
            if (part.isEmpty()) return false;
        }
        return true;
    }
}
