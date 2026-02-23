package org.example.consumer.model.device;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "devices")
public class Device {

    @Id
    @JsonProperty("mac_address")
    private String macAddress;

    @JsonProperty("device_name")
    private String deviceName;

    private String owner;
}
