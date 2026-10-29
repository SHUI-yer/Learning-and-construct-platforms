package com.learning.assessment.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExamAnswer {

    private Long id;
    private Long submissionId;
    private Long qId;
    private String studentAnswer;
    private Integer isCorrect;
    private BigDecimal score;
    private LocalDateTime answeredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}