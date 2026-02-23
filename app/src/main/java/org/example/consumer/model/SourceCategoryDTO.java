package org.example.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceCategoryDTO {
    private String id;
    private String name;
    private List<SourceCategoryDTO> subcategories;
}
