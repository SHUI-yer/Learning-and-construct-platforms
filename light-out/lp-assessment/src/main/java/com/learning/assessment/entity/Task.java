package com.learning.assessment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    private Long taskId;
    private Long courseId;
    private Long chapterId;
    private String title;
    private String description;
    private String taskType;
    private String tag;
    private BigDecimal weight;
    private String publishType;
    private LocalDateTime publishTime;
    private LocalDateTime deadline;
    private Integer allowLate;
    private BigDecimal latePenalty;
    private BigDecimal maxScore;
    private Integer allowResubmit;
    private String attachmentUrls;
    private LocalDateTime deletedAt;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}