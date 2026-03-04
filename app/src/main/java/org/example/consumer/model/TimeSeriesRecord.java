package org.example.consumer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "time_series_record")
public class TimeSeriesRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String key;

    @Column(nullable = false)
    private float value;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private Producer producer;

    @Column(name = "read_time", nullable = false)
    private Instant readTime;
}
