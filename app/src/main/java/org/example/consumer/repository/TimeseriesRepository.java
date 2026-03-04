package org.example.consumer.repository;

import org.example.consumer.model.TimeSeriesRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeseriesRepository extends JpaRepository<TimeSeriesRecord, Long> {
    List<TimeSeriesRecord> findByProducerStreamId(Long streamId);
}
