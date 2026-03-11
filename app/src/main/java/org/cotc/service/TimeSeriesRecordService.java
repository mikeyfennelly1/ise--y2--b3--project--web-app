package org.cotc.service;

import org.cotc.model.Group;
import org.cotc.model.Producer;
import org.cotc.model.TimeSeriesRecord;
import org.cotc.utils.Translators;
import org.cotc.libcotc.dto.ProducerDTO;
import org.cotc.libcotc.dto.GroupDTO;
import org.cotc.libcotc.dto.TimeSeriesRecordDTO;
import org.cotc.repository.ProducerRepository;
import org.cotc.repository.GroupRepository;
import org.cotc.repository.TimeseriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TimeSeriesRecordService {

    private static final Logger logger = LoggerFactory.getLogger(TimeSeriesRecordService.class);

    private final GroupRepository groupRepository;
    private final ProducerRepository producerRepository;
    private final TimeseriesRepository timeseriesRepository;

    @Autowired
    public TimeSeriesRecordService(
            GroupRepository groupRepository,
            ProducerRepository producerRepository,
            TimeseriesRepository timeseriesRepository,
            Translators translators
    ) {
        this.groupRepository = groupRepository;
        this.producerRepository = producerRepository;
        this.timeseriesRepository = timeseriesRepository;
    }

    public List<GroupDTO> getAllStreams() {
        logger.debug("getAllStreams - querying repository");
        List<GroupDTO> streams = groupRepository.findAll().stream()
                .map(Group::toDTO)
                .toList();
        logger.debug("getAllStreams - returning {} stream(s)", streams.size());
        return streams;
    }

    public List<GroupDTO> getGroupHierarchy() {
        logger.debug("getGroupHierarchy - querying repository");
        List<Group> allGroups = groupRepository.findAll();
        logger.debug("getGroupHierarchy - building DTO map from {} stream(s)", allGroups.size());
        List<GroupDTO> roots = Translators.hierarchyFromGroups(allGroups);
        logger.debug("getGroupHierarchy - returning {} root stream(s)", roots.size());
        return roots;
    }

    public List<TimeSeriesRecordDTO> getRecordsByGroupId(String groupName) {
        logger.debug("getRecordsByStreamId - querying for streamId={}", groupName);
        List<TimeSeriesRecordDTO> records = timeseriesRepository.findByProducerGroupName(groupName).stream()
                .map(TimeSeriesRecord::toDTO)
                .toList();
        logger.debug("getRecordsByStreamId - returning {} record(s) for streamId={}", records.size(), groupName);
        return records;
    }

}
