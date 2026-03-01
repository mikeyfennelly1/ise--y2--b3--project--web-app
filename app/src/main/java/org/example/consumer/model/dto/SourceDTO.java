package org.example.consumer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SourceDTO {
    private Long id;
    private String sourceName;
}
