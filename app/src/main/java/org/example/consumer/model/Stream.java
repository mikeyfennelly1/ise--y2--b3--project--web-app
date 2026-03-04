package org.example.consumer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stream", uniqueConstraints = @UniqueConstraint(name = "uq_stream_name", columnNames = "name"))
public class Stream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "stream", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Producer> producers;
}
