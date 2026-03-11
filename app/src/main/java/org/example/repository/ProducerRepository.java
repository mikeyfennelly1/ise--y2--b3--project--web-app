package org.example.repository;

import org.example.model.Producer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProducerRepository extends JpaRepository<Producer, UUID> {
    List<Producer> findByStreamName(String streamName);
    List<Producer> findByStreamUuid(UUID uuid);
    Producer findByName(String producerName);
}
