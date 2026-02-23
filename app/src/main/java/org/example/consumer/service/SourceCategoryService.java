package org.example.consumer.service;

import org.example.consumer.model.SourceCategory;
import org.example.consumer.model.SourceCategoryDTO;
import org.example.consumer.model.device.DeviceType;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SourceCategoryService {

    public List<SourceCategoryDTO> getAvailableCategoryDTOs() {
        return Arrays.stream(SourceCategory.values())
                .map(c -> new SourceCategoryDTO(c.name(), c.name(), getSubcategoriesByCategory(c)))
                .collect(Collectors.toList());
    }

    public List<SourceCategoryDTO> getSubcategoriesByCategory(SourceCategory category) {
        return switch (category) {
            case device -> Arrays.stream(DeviceType.values())
                    .map(t -> new SourceCategoryDTO(t.name(), t.name(), List.of()))
                    .collect(Collectors.toList());
            case third_party -> List.of();
        };
    }

    public String getAvailableCategoriesAsString() {
        return Arrays.stream(SourceCategory.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
