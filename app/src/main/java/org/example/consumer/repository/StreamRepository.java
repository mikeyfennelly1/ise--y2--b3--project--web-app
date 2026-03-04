package org.example.consumer.repository;

import org.example.consumer.model.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface StreamRepository extends JpaRepository<Stream, Long> {

    @Query("SELECT s FROM Stream s WHERE s.name LIKE CONCAT(:subjectName, '.%') AND s.name NOT LIKE CONCAT(:subjectName, '.%.%')")
    List<Stream> getChildren(@Param("subjectName") String subjectName);

    @Query("SELECT COUNT(s) > 0 FROM Stream s WHERE s.name = :streamName")
    boolean streamExists(@Param("streamName") String streamName);

    default Stream newStream(String name) {
        return save(new Stream(null, name, new java.util.ArrayList<>()));
    }

    @Transactional
    void deleteByName(String name);

    @Modifying
    @Transactional
    @Query("DELETE FROM Stream s WHERE s.name LIKE CONCAT(:name, '.%')")
    void deleteAllDescendants(@Param("name") String name);
}
