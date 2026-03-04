package org.example.consumer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class TimeSeriesRecordDTO {
    private Long id;
    private String key;
    private float value;
    private String producerName;
    private Instant readTime;
}
