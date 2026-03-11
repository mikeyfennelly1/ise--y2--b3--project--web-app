package org.cotc.service;

import jakarta.annotation.PostConstruct;
import org.cotc.exception.GroupAlreadyExistsException;
import org.cotc.exception.InvalidGroupNameException;
import org.cotc.exception.GroupNotFoundException;
import org.cotc.libcotc.dto.GroupDTO;
import org.cotc.libcotc.dto.TimeSeriesMessageDTO;
import org.cotc.model.Group;
import org.cotc.repository.GroupRepository;
import org.cotc.repository.TimeseriesRepository;
import org.cotc.utils.GroupNameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;

@Service
public class GroupService {
    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
    private final GroupRepository groupRepository;
    private final Set<String> groupNameCache = new HashSet<>();
    private final GroupNameUtils groupNameUtils;
    private final TimeseriesRepository timeseriesRepository;

    @Autowired
    GroupService(
            GroupRepository groupRepository,
            GroupNameUtils groupNameUtils, TimeseriesRepository timeseriesRepository) {
        this.groupRepository = groupRepository;
        this.groupNameUtils = groupNameUtils;
        this.timeseriesRepository = timeseriesRepository;
    }

    @PostConstruct
    private void restoreGroupCache() {
        // get all the groups, put them into the cache
    }

    public Optional<GroupDTO> getGroupByName(String name) {
        logger.debug("getGroupByName - querying for name='{}'", name);
        return Optional.ofNullable(groupRepository.findByName(name)).map(Group::toDTO);
    }

    public String getGroupNameById(UUID groupId) {
        logger.debug("getGroupNameById - querying for streamId={}", groupId);
        return groupRepository.findById(groupId)
                .map(Group::getName)
                .orElse(null);
    }

    public void deleteGroup(String name) throws GroupNotFoundException {
        logger.debug("deleteGroup - name='{}'", name);
        if (!groupRepository.groupExists(name)) {
            throw new GroupNotFoundException(name);
        }
        groupRepository.deleteAllDescendants(name);
        logger.debug("deleteGroup - deleted all descendants of '{}'", name);
        groupRepository.deleteByName(name);
        logger.debug("deleteGroup - stream '{}' deleted successfully", name);
    }

    public List<String> getAllGroupNames() {
        logger.debug("getAllGroupNames - fetching all group names");
        List<String> names = groupRepository.findAll().stream()
                .map(stream -> stream.getName())
                .toList();
        logger.debug("getAllGroupNames - returning {} group names", names.size());
        return names;
    }

    public List<String> getDescendantGroups(String groupName) throws InvalidGroupNameException, GroupNotFoundException {
        logger.debug("getDescendantGroups - parentPath='{}'", groupName);
        if (!groupRepository.groupExists(groupName)) {
            logger.debug("getDescendantGroups - stream '{}' not found, throwing TreePathNotFoundException", groupName);
            throw new GroupNotFoundException(groupName);
        }
        List<String> children = groupRepository.getDescendants(groupName).stream()
                .map(stream -> stream.getName())
                .toList();
        logger.debug("getDescendantGroups - found {} children for '{}'", children.size(), groupName);
        return children;
    }

    public boolean groupExists(String streamName) {
        // this is assumed to be a valid source of truth, as groups are
        // read from database every time application starts, and thereon
        // the amount of group CRUD operations where cache becomes
        // inconsistent is considered negligible.
        // Bear in mind this application has 1 running instance at any time.
        return this.groupNameCache.contains(streamName);
    }

    public GroupDTO createGroup(String groupName) throws InvalidGroupNameException, GroupAlreadyExistsException, GroupNotFoundException {
        InvalidGroupNameException.validate(groupName);
        if(groupExists(groupName)) throw new GroupAlreadyExistsException(groupName);
        if (!groupNameUtils.isRootName(groupName) && !groupExists(groupNameUtils.getParentName(groupName))) {
            throw new GroupNotFoundException(groupNameUtils.getParentName(groupName));
        }

        Group group = new Group();
        group.setName(groupName);
        groupRepository.save(group);
        return group.toDTO();
    }

    public Flux<ServerSentEvent<TimeSeriesMessageDTO>> getGroupFlux(String groupName) throws GroupNotFoundException {
        if (!groupExists(groupName)) throw new GroupNotFoundException(groupName);
        return null;
    }

}
