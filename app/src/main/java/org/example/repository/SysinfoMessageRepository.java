package org.example.repository;

import org.example.model.SysinfoMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface SysinfoMessageRepository extends JpaRepository<SysinfoMessage, Long> {

    @Query("SELECT DISTINCT s.macAddress FROM SysinfoMessage s")
    List<String> findDistinctMacAddresses();

    List<SysinfoMessage> findByMacAddress(String macAddress);

    boolean existsByMacAddress(String macAddress);
}