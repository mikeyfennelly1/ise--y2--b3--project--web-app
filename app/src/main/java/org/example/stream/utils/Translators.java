package org.example.stream.utils;

import lombok.NonNull;
import org.example.model.Producer;
import org.example.model.Stream;
import org.example.libb3project.dto.ProducerDTO;
import org.example.libb3project.dto.StreamDTO;
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

    public static @NonNull List<StreamDTO> hierarchyFromStreams(List<Stream> allStreams) {
        Map<String, StreamDTO> dtoMap = streamNameToDtoMap(allStreams);
        return hierarchyFromDtoMap(dtoMap);
    }

    private static @NonNull List<StreamDTO> hierarchyFromDtoMap(Map<String, StreamDTO> dtoMap) {
        // Attach each node to its parent; collect nodes with no parent as roots
        List<StreamDTO> roots = new ArrayList<>();
        for (Map.Entry<String, StreamDTO> entry : dtoMap.entrySet()) {
            String name = entry.getKey();
            StreamDTO dto = entry.getValue();
            int lastDot = name.lastIndexOf('.');
            if (lastDot == -1) {
                logger.trace("hierarchyFromDtoMap - '{}' is a root stream", name);
                roots.add(dto);
            } else {
                String parentName = name.substring(0, lastDot);
                StreamDTO parent = dtoMap.get(parentName);
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

    private static @NonNull Map<String, StreamDTO> streamNameToDtoMap(List<Stream> allStreams) {
        Map<String, StreamDTO> dtoMap = new LinkedHashMap<>();
        for (Stream stream : allStreams) {
            List<ProducerDTO> producers = stream.getProducers().stream()
                    .map(Producer::toDTO)
                    .toList();
            logger.trace("hierarchyFromStreams - mapped stream '{}' with {} producer(s)", stream.getName(), producers.size());
            dtoMap.put(stream.getName(), stream.toDTO());
        }
        return dtoMap;
    }

}
