package org.example.stream.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import org.example.model.Producer;
import org.example.model.TimeSeriesRecord;
import org.example.repository.ProducerRepository;
import org.example.repository.TimeseriesRepository;
import org.example.libb3project.dto.TimeSeriesMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

class TimeSeriesStreamHandler implements MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesStreamHandler.class);
    private static final String DLQ_SUBJECT_PREFIX = "_DLQ.";

    private final ObjectMapper objectMapper;
    private final TimeseriesRepository timeseriesRepository;
    private final ProducerRepository producerRepository;
    private final NatsConnectionSingleton nats;

    private Sinks.Many<TimeSeriesMessageDTO> sink;
    private Flux<TimeSeriesMessageDTO> flux;

    TimeSeriesStreamHandler(
            ObjectMapper objectMapper,
            TimeseriesRepository timeseriesRepository,
            ProducerRepository producerRepository,
            NatsConnectionSingleton nats
    ) {
        this.objectMapper = objectMapper;
        this.timeseriesRepository = timeseriesRepository;
        this.producerRepository = producerRepository;
        this.nats = nats;
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
        this.flux = sink.asFlux();
        this.nats.getConnection().subscribe("test");
    }

    @Override
    public void onMessage(Message msg) throws InterruptedException {
        logger.debug("received message {}", msg);
        try {
            TimeSeriesMessageDTO dto = readToDto(msg);
            Producer producer = getProducerByName(dto.getProducerName());
            List<TimeSeriesRecord> recordList = splitIntoRecordList(dto, producer);
            saveAllRecords(recordList);
            writeDTOToFlux(dto);
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

    private void writeDTOToFlux(TimeSeriesMessageDTO dto) {
        Sinks.EmitResult result = sink.tryEmitNext(dto);
        if (result.isFailure()) {
            logger.warn("writeDTOToFlux - failed to emit dto (result={})", result);
        }
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
            throw new Exception("could not find producer with name: " + name);
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
}
