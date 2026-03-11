package org.example.repository;

import org.example.model.TimeSeriesRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimeseriesRepository extends JpaRepository<TimeSeriesRecord, UUID> {
    List<TimeSeriesRecord> findByProducerStreamUuid(UUID streamId);
    List<TimeSeriesRecord> findByProducerStreamName(String streamName);
}
