package com.learning.assessment.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QuestionBank {

    private Long qId;
    private Long courseId;
    private Long creatorId;
    private String qType;
    private String content;
    private String options;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private String tags;
    private String source;
    private Long importId;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}