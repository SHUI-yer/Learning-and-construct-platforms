package com.learning.assessment.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GradeAppeal {

    private Long appealId;
    private Long gradeId;
    private Long studentId;
    private String appealReason;
    private String teacherResponse;
    private BigDecimal newScore;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private Integer version;
    private LocalDateTime updatedAt;
}