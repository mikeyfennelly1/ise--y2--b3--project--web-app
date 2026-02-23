package org.example.consumer.repository;

import org.example.consumer.model.device.SysinfoMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface TimeseriesRepository extends JpaRepository<SysinfoMessage, Long> {

    @Query("SELECT DISTINCT s.macAddress FROM SysinfoMessage s")
    List<String> findDistinctMacAddresses();

    List<SysinfoMessage> findByMacAddress(String macAddress);

    List<SysinfoMessage> findByMacAddressAndReadTimeBetween(String macAddress, long startTime, long endTime);

    boolean existsByMacAddress(String macAddress);
}