package org.example.consumer.stream.utils;

import org.example.consumer.stream.exception.InvalidSubscriptionTreePathFormatException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SubscriptionNameUtils {
    public NameBuilder builder(String rootName) {
        return new NameBuilder(rootName);
    }

    public static class NameBuilder {
        private final StringBuilder fullName;

        private NameBuilder(String rootName) {
            this.fullName = new StringBuilder().append(rootName);
        }

        public void addName(String name) throws InvalidSubscriptionTreePathFormatException {
            InvalidSubscriptionTreePathFormatException.validate(name);
            fullName.append(".").append(name);
        }

        public String build() {
            return fullName.toString();
        }
    }

    /**
     * Splits a subject name by full stop into an ordered sequence of its tokens.
     * e.g. "device.sysinfo" → ["device", "sysinfo"], "device" → ["device"]
     *
     * @return an unmodifiable, order-preserving sequence of the subject's tokens.
     * @throws InvalidSubscriptionTreePathFormatException if the subject is not a valid format.
     */
    public static List<String> listOfSubjectsFromTreePath(String subject) throws InvalidSubscriptionTreePathFormatException {
        InvalidSubscriptionTreePathFormatException.validate(subject);
        if (subject.contains(".")) {
            return List.of(subject.split("\\."));
        } else {
            return List.of(subject);
        }
    }
}
