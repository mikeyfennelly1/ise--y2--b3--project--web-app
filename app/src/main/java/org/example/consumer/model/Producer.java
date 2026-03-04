package org.example.consumer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "producer")
public class Producer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producer_name", nullable = false)
    private String sourceName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stream_id", nullable = false)
    private Stream stream;
}
