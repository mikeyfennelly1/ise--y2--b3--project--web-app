package org.cotc.utils;

import lombok.NonNull;
import org.cotc.model.Group;
import org.cotc.model.Producer;
import org.cotc.libcotc.dto.ProducerDTO;
import org.cotc.libcotc.dto.GroupDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class Translators {
    private static final Logger logger = LoggerFactory.getLogger(Translators.class);

    public static @NonNull List<GroupDTO> hierarchyFromGroups(List<Group> allGroups) {
        Map<String, GroupDTO> dtoMap = groupNameToDtoMap(allGroups);
        return hierarchyFromDtoMap(dtoMap);
    }

    private static @NonNull List<GroupDTO> hierarchyFromDtoMap(Map<String, GroupDTO> dtoMap) {
        // Attach each node to its parent; collect nodes with no parent as roots
        List<GroupDTO> roots = new ArrayList<>();
        for (Map.Entry<String, GroupDTO> entry : dtoMap.entrySet()) {
            String name = entry.getKey();
            GroupDTO dto = entry.getValue();
            int lastDot = name.lastIndexOf('.');
            if (lastDot == -1) {
                logger.trace("hierarchyFromDtoMap - '{}' is a root stream", name);
                roots.add(dto);
            } else {
                String parentName = name.substring(0, lastDot);
                GroupDTO parent = dtoMap.get(parentName);
                if (parent != null) {
                    logger.trace("hierarchyFromDtoMap - attaching '{}' as child of '{}'", name, parentName);
                    parent.getChildren().add(dto);
                } else {
                    logger.trace("hierarchyFromDtoMap - parent '{}' not found for '{}', treating as root", parentName, name);
                    roots.add(dto);
                }
            }
        }
        return roots;
    }

    private static @NonNull Map<String, GroupDTO> groupNameToDtoMap(List<Group> allGroups) {
        Map<String, GroupDTO> dtoMap = new LinkedHashMap<>();
        for (Group group : allGroups) {
            List<ProducerDTO> producers = group.getProducers().stream()
                    .map(Producer::toDTO)
                    .toList();
            logger.trace("groupNameToDtoMap - mapped stream '{}' with {} producer(s)", group.getName(), producers.size());
            dtoMap.put(group.getName(), group.toDTO());
        }
        return dtoMap;
    }

}
