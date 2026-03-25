package org.cotc.repository;

import org.cotc.model.TimeSeriesRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimeseriesRepository extends JpaRepository<TimeSeriesRecord, UUID> {
    List<TimeSeriesRecord> findByProducerGroupUuid(UUID groupUuid);
    List<TimeSeriesRecord> findByProducerGroupName(String groupName);

    @Modifying
    @Transactional
    @Query("DELETE FROM TimeSeriesRecord t WHERE t.producer.group.name = :groupName")
    void deleteByProducerGroupName(@Param("groupName") String groupName);

    @Modifying
    @Transactional
    @Query("DELETE FROM TimeSeriesRecord t WHERE t.producer.group.name LIKE CONCAT(:groupName, '.%')")
    void deleteByProducerGroupNameDescendants(@Param("groupName") String groupName);
}
