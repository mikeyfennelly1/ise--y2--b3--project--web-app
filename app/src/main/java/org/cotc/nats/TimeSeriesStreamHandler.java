package org.cotc.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import org.cotc.config.NatsConfiguration;
import org.cotc.model.Producer;
import org.cotc.model.TimeSeriesRecord;
import org.cotc.service.ProducerService;
import org.cotc.repository.TimeseriesRepository;
import org.cotc.libcotc.dto.TimeSeriesMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

@Component
class TimeSeriesStreamHandler implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesStreamHandler.class);
    private static final String DLQ_SUBJECT_PREFIX = "_DLQ.";

    private final ObjectMapper objectMapper;
    private final TimeseriesRepository timeseriesRepository;
    private final ProducerService producerService;
    private final Connection nc;

    TimeSeriesStreamHandler(
            ObjectMapper objectMapper,
            TimeseriesRepository timeseriesRepository,
            NatsConfiguration natsConfiguration,
            ProducerService producerService
    ) {
        this.objectMapper = objectMapper;
        this.timeseriesRepository = timeseriesRepository;
        this.producerService = producerService;
        this.nc = natsConfiguration.getConnection();
    }

    @Override
    public void onMessage(Message msg) throws InterruptedException {
        logger.debug("received message {}", msg);
        try {
            TimeSeriesMessageDTO dto = natsMessageToTimeSeriesDto(msg);
            Producer producer = producerService.getProducerByName(dto.getProducerName());
            List<TimeSeriesRecord> recordList = splitIntoRecordList(dto, producer);
            saveAllRecords(recordList);
            msg.ack();
        } catch (Exception e) {
            logger.error("unrecoverable error while processing message: {}", e.getMessage());
            sendToDeadLetterQueue(msg);
            msg.ack();
        }
    }

    private void saveAllRecords(List<TimeSeriesRecord> recordList) throws InterruptedException {
        int n = recordList.size();
        logger.debug("submitting {} restore task(s) to virtual thread executor", n);
        CountDownLatch latch = new CountDownLatch(n);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (TimeSeriesRecord record : recordList) {
                executor.submit(() -> {
                    timeseriesRepository.save(record);
                    latch.countDown();
                });
            }
        }
        logger.debug("waiting for all restore tasks to complete");
        latch.await();
        logger.debug("all {} stream(s) restored successfully", n);
    }

    private void sendToDeadLetterQueue(Message message) {
        String dlqSubject = DLQ_SUBJECT_PREFIX + message.getSubject();
        logger.warn("sendToDeadLetterQueue - forwarding failed message to '{}'", dlqSubject);
        try {
            nc.publish(dlqSubject, message.getData());
        } catch (Exception e) {
            logger.error("sendToDeadLetterQueue - failed to publish to DLQ subject '{}': {}", dlqSubject, e.getMessage());
        }
    }

    private TimeSeriesMessageDTO natsMessageToTimeSeriesDto(Message message) throws Exception {
        TimeSeriesMessageDTO dto = null;
        try {
            dto = objectMapper.readValue(message.getData(), TimeSeriesMessageDTO.class);
        } catch (IOException e) {
            logger.debug("failed to read message (type={}): {}", e.getClass(), e.getMessage());
        }
        return dto;
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
}
