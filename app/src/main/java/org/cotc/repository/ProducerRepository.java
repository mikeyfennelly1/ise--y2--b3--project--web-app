package org.cotc.repository;

import org.cotc.model.Producer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProducerRepository extends JpaRepository<Producer, UUID> {
    List<Producer> findByGroupName(String groupName);
    List<Producer> findByGroupUuid(UUID uuid);
    Producer findByName(String producerName);
}
