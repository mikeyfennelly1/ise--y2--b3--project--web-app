package org.example.consumer.repository;

import org.example.consumer.model.Producer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProducerRepository extends JpaRepository<Producer, Long> {
    List<Producer> findByStreamName(String streamName);
    List<Producer> findByStreamId(Long streamId);
}
