package com.learning.assessment.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GradeScheme {

    private Long schemeId;
    private Long courseId;
    private String componentType;
    private BigDecimal weight;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}