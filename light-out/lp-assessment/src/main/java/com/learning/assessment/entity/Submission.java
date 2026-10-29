package com.learning.assessment.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Submission {

    private Long submissionId;
    private Long taskId;
    private Long studentId;
    private String submitType;
    private String content;
    private String attachmentUrls;
    private LocalDateTime submittedAt;
    private Integer isLate;
    // 列 is_excellent 为 NOT NULL：insert 语句显式写入该字段，未初始化会导致插入失败
    private Integer isExcellent = 0;
    private String status;
    private String teacherComment;
    private BigDecimal score;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}