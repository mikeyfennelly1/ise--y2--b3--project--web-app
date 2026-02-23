package org.example.consumer.model.device;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sysinfo_messages")
public class SysinfoMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("mac_address")
    private String macAddress;
    @JsonProperty("read_time")
    private long readTime;

    @ElementCollection
    @CollectionTable(name = "sysinfo_metrics", joinColumns = @JoinColumn(name = "message_id"))
    @MapKeyColumn(name = "metric_name")
    @Column(name = "metric_value")
    @JsonProperty("metrics")
    private Map<String, Double> metrics;
}
