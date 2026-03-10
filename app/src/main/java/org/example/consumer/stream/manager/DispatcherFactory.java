package org.example.consumer.stream.manager;

import io.nats.client.MessageHandler;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
class DispatcherFactory {
    private DefaultDispatchMessageHandler defaultDispatchMessageHandler;

    MessageHandler getMessageHandler(String name) {
        switch (name) {
            default:
                return defaultDispatchMessageHandler;
        }
    }
}
