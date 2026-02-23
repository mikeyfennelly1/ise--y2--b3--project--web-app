package org.example.model.source.device;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MacAddressMetadata {
    @JsonProperty("mac_address")
    private String macAddress;
}
