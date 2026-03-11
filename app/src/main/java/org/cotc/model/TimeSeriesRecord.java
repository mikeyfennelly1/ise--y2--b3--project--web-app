package org.cotc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cotc.libcotc.dto.TimeSeriesRecordDTO;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "time_series_record")
public class TimeSeriesRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String key;

    @Column(nullable = false)
    private float value;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producer_id", nullable = false)
    private Producer producer;

    @Column(name = "read_time", nullable = false)
    private Instant readTime;

    @JsonIgnore
    public TimeSeriesRecordDTO toDTO() {
        return new TimeSeriesRecordDTO(
                this.id,
                this.key,
                this.getValue(),
                this.producer.getName(),
                this.readTime
        );
    }
}
