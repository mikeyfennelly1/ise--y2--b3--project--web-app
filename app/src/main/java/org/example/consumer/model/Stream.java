package org.example.consumer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.libb3project.dto.ProducerDTO;
import org.example.libb3project.dto.StreamDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "stream", uniqueConstraints = @UniqueConstraint(name = "uq_stream_name", columnNames = "name"))
public class Stream {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID uuid;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "stream", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Producer> producers;

    @JsonIgnore
    public StreamDTO toDTO() {
        List<ProducerDTO> producers = this.getProducers().stream()
                .map(Producer::toDTO)
                .toList();
        return new StreamDTO(this.uuid, this.name, new ArrayList<>(), producers);
    }
}
