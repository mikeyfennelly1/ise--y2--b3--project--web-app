package org.cotc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import jakarta.annotation.PostConstruct;
import org.cotc.config.NatsConfiguration;
import org.cotc.exception.GroupAlreadyExistsException;
import org.cotc.exception.InvalidGroupNameException;
import org.cotc.exception.GroupNotFoundException;
import org.cotc.libcotc.dto.GroupDTO;
import org.cotc.libcotc.dto.TimeSeriesMessageDTO;
import org.cotc.model.Group;
import org.cotc.repository.GroupRepository;
import org.cotc.repository.ProducerRepository;
import org.cotc.repository.TimeseriesRepository;
import org.cotc.utils.GroupNameUtils;
import org.cotc.utils.Translators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;
import reactor.core.publisher.Flux;

import java.util.*;

@Service
@CrossOrigin
public class GroupService {
    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
    private static final String TIMESERIES_SUBJECT = "TIMESERIES";
    private final GroupRepository groupRepository;
    private final ProducerRepository producerRepository;
    private final TimeseriesRepository timeseriesRepository;
    private final Set<String> groupNameCache = new HashSet<>();
    private final GroupNameUtils groupNameUtils;
    private final Connection natsConnection;
    private final ObjectMapper objectMapper;
    private final Translators translators;

    @Autowired
    GroupService(
            GroupRepository groupRepository,
            ProducerRepository producerRepository,
            TimeseriesRepository timeseriesRepository,
            GroupNameUtils groupNameUtils,
            NatsConfiguration natsConfiguration,
            ObjectMapper objectMapper,
            Translators translators
    ) {
        this.groupRepository = groupRepository;
        this.producerRepository = producerRepository;
        this.timeseriesRepository = timeseriesRepository;
        this.groupNameUtils = groupNameUtils;
        this.natsConnection = natsConfiguration.getConnection();
        this.objectMapper = objectMapper;
        this.translators = translators;
    }

    @PostConstruct
    private void restoreGroupCache() {
        // get all the groups, put them into the cache
        this.groupNameCache.addAll(this.getAllGroupNames());
    }

    public Optional<GroupDTO> getGroupByName(String name) {
        logger.debug("getGroupByName - querying for name='{}'", name);
        return Optional.ofNullable(groupRepository.findByName(name)).map(Group::toDTO);
    }

    public void deleteGroup(String name) throws GroupNotFoundException {
        logger.debug("deleteGroup - name='{}'", name);
        if (!groupRepository.groupExists(name)) {
            throw new GroupNotFoundException(name);
        }
        timeseriesRepository.deleteByProducerGroupNameDescendants(name);
        producerRepository.deleteByGroupNameDescendants(name);
        groupRepository.deleteAllDescendants(name);
        logger.debug("deleteGroup - deleted all descendants of '{}'", name);
        timeseriesRepository.deleteByProducerGroupName(name);
        producerRepository.deleteByGroupName(name);
        groupRepository.deleteByName(name);
        groupNameCache.remove(name);
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

    public List<GroupDTO> getFullGroupHierarchy() {
        List<Group> allGroups = groupRepository.findAll();
        return translators.hierarchyFromGroups(allGroups);
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
        if(groupExists(groupName)) {
            logger.debug("group already exists for group: {}", groupName);
            throw new GroupAlreadyExistsException(groupName);
        } else {
            if (!groupNameUtils.isRootName(groupName) && !groupExists(groupNameUtils.getParentName(groupName))) {
                throw new GroupNotFoundException(groupNameUtils.getParentName(groupName));
            }

            Group group = new Group();
            group.setName(groupName);
            group.setUuid(UUID.randomUUID());
            group.setProducers(List.of());
            groupRepository.save(group);
            this.groupNameCache.add(group.getName());
            logger.debug("created group: {}", groupName);
            return group.toDTO();
        }
    }

    public Flux<ServerSentEvent<TimeSeriesMessageDTO>> getGroupFlux(String groupName) throws GroupNotFoundException {
        if (!groupExists(groupName)) throw new GroupNotFoundException(groupName);
        return Flux.create(sink -> {
            Dispatcher dispatcher = natsConnection.createDispatcher(msg -> {
                try {
                    TimeSeriesMessageDTO dto = objectMapper.readValue(msg.getData(), TimeSeriesMessageDTO.class);
                    sink.next(ServerSentEvent.builder(dto).build());
                } catch (Exception e) {
                    logger.error("getGroupFlux - failed to deserialize message: {}", e.getMessage());
                }
            });
            dispatcher.subscribe(TIMESERIES_SUBJECT);
            sink.onCancel(() -> {
                try {
                    natsConnection.closeDispatcher(dispatcher);
                } catch (Exception e) {
                    logger.error("getGroupFlux - failed to close dispatcher: {}", e.getMessage());
                }
            });
        });
    }

}
