package org.example.consumer.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.consumer.model.Producer;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesMessageDTO {

    @JsonProperty("read_time")
    private long readTime;

    @JsonProperty("values")
    private Map<String, Double> values;

    @JsonProperty("source_properties")
    private Producer producer;
}
