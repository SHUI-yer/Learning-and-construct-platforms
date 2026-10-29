package com.learning.assessment.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExamPaper {

    private Long paperId;
    private Long courseId;
    private Long taskId;
    private String title;
    private BigDecimal totalScore;
    private Integer duration;
    private Integer shuffleQuestions;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}