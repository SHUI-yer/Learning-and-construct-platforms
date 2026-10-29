package com.learning.assessment.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QuestionImport {

    private Long id;
    private Long courseId;
    private Long operatorId;
    private String fileUrl;
    private String fileFormat;
    private Integer importCount;
    private Integer successCount;
    private Integer failCount;
    private String errorDetail;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}