package org.cotc.repository;

import org.cotc.model.Producer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProducerRepository extends JpaRepository<Producer, UUID> {
    List<Producer> findByGroupName(String groupName);
    List<Producer> findByGroupUuid(UUID uuid);
    Producer findByName(String producerName);

    @Modifying
    @Transactional
    @Query("DELETE FROM Producer p WHERE p.group.name = :groupName")
    void deleteByGroupName(@Param("groupName") String groupName);

    @Modifying
    @Transactional
    @Query("DELETE FROM Producer p WHERE p.group.name LIKE CONCAT(:groupName, '.%')")
    void deleteByGroupNameDescendants(@Param("groupName") String groupName);
}
