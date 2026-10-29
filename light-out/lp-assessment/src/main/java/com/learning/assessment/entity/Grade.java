package com.learning.assessment.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Grade {

    private Long gradeId;
    private Long courseId;
    private Long studentId;
    private Long taskId;
    private Long submissionId;
    private BigDecimal score;
    private BigDecimal weightedScore;
    private String componentType;
    private Integer isConfirmed;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}