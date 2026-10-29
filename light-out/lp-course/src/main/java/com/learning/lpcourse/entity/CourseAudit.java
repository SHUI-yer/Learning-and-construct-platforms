package com.learning.lpcourse.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CourseAudit {
    private Long id;
    /**
     * 课程id
     */
    private Long courseId;
    /**
     * 审核人id
     */
    private Long auditorId;
    /**
     * 审核状态：1通过 2驳回
     */
    private Integer auditStatus;
    /**
     * 审核备注
     */
    private String auditRemark;
    /**
     * 审核时间
     */
    private LocalDateTime auditedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}