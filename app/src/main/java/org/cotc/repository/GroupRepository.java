package org.cotc.repository;

import org.cotc.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    @Query("SELECT s FROM Group s WHERE s.name LIKE CONCAT(:groupName, '.%') AND s.name NOT LIKE CONCAT(:groupName, '.%.%')")
    List<Group> getDescendants(@Param("groupName") String groupName);

    @Query("SELECT COUNT(s) > 0 FROM Group s WHERE s.name = :groupName")
    boolean groupExists(@Param("groupName") String groupName);

    default Group newStream(String name) {
        return save(Group.builder().name(name).producers(new java.util.ArrayList<>()).build());
    }

    Group findByName(String name);

    @Transactional
    void deleteByName(String name);

    @Modifying
    @Transactional
    @Query("DELETE FROM Group s WHERE s.name LIKE CONCAT(:name, '.%')")
    void deleteAllDescendants(@Param("name") String name);
}
