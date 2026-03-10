package org.example.consumer.stream.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.AllArgsConstructor;
import org.example.consumer.model.Producer;
import org.example.consumer.model.TimeSeriesRecord;
import org.example.consumer.repository.ProducerRepository;
import org.example.consumer.repository.TimeseriesRepository;
import org.example.libb3project.dto.TimeSeriesMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@Component
class DefaultDispatchMessageHandler implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(DefaultDispatchMessageHandler.class);
    private static final String DLQ_SUBJECT_PREFIX = "_DLQ.";

    private final ObjectMapper objectMapper;
    private final TimeseriesRepository timeseriesRepository;
    private final ProducerRepository producerRepository;
    private final NatsConnectionSingleton nats;

    @Override
    public void onMessage(Message message) throws InterruptedException {
        logger.debug("received message {}", message);
        try {
            TimeSeriesMessageDTO dto = readToDto(message);
            Producer producer = getProducerByName(dto.getProducerName());
            List<TimeSeriesRecord> recordList = splitIntoRecordList(dto, producer);
            ackMessage();
        } catch (Exception e) {
            logger.error("unrecoverable error while processing message");
            sendToDeadLetterQueue(message);
            ackMessage();
        }
    }

    private void writeDTOToFlux() {

    }

    private void sendToDeadLetterQueue(Message message) {
        String dlqSubject = DLQ_SUBJECT_PREFIX + message.getSubject();
        logger.warn("sendToDeadLetterQueue - forwarding failed message to '{}'", dlqSubject);
        try {
            nats.getConnection().publish(dlqSubject, message.getData());
        } catch (Exception e) {
            logger.error("sendToDeadLetterQueue - failed to publish to DLQ subject '{}': {}", dlqSubject, e.getMessage());
        }
    }

    private void ackMessage() {
        // Core NATS (non-JetStream) subscriptions are fire-and-forget; no explicit ack is required.
        logger.debug("ackMessage - message acknowledged");
    }

    private TimeSeriesMessageDTO readToDto(Message message) throws Exception {
        TimeSeriesMessageDTO dto = null;
        try {
            dto = objectMapper.readValue(message.getData(), TimeSeriesMessageDTO.class);
        } catch (IOException e) {
            logger.debug("failed to read message (type={}): {}", e.getClass(), e.getMessage());
        }
        return dto;
    }

    private Producer getProducerByName(String name) throws Exception {
        Producer producer = null;
        try {
            producer = producerRepository.findByName(name);
        } catch (Exception e) {
            logger.error("could not find producer with name={}", name);
        }
        if (producer == null) {
            logger.error("read null producer from database for producer name={}", name);
        }
        return producer;
    }

    private List<TimeSeriesRecord> splitIntoRecordList(TimeSeriesMessageDTO dto, Producer producer) {
        List<TimeSeriesRecord> recordList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : dto.getValues().entrySet()) {
            TimeSeriesRecord record = TimeSeriesRecord.builder()
                    .key(entry.getKey())
                    .value(entry.getValue().floatValue())
                    .producer(producer)
                    .readTime(Instant.ofEpochSecond(dto.getReadTime()))
                    .id(UUID.randomUUID())
                    .build();
            recordList.add(record);
        }
        return recordList;
    }

    private void processTimeSeriesRecord(TimeSeriesRecord record) {
        timeseriesRepository.save(record);
    }
}
