package com.learning.learning.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class LearningReportVO {

    private Long reportId;
    private String reportType;
    private String period;
    private Map<String, Object> data;
    private LocalDateTime generatedAt;
}
