package com.learning.assessment.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TaskCreateDTO {

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
}