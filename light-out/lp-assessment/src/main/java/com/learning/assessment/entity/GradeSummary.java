package com.learning.assessment.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GradeSummary {

    private Long id;
    private Long courseId;
    private Long studentId;
    private BigDecimal finalScore;
    private Integer rankInCourse;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private Integer isExported;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}