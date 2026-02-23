package org.example.model.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataSource {
    @JsonProperty("source_id")
    private String sourceType;

    @JsonProperty("mac_address")
    private String macAddress;
}
