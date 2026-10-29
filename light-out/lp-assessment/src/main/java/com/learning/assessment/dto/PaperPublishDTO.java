package com.learning.assessment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaperPublishDTO {

    private Long paperId;
    private Long courseId;
    private String title;
    private String description;
    // 任务类型：quiz-测验/考试 assignment-作业
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
}
