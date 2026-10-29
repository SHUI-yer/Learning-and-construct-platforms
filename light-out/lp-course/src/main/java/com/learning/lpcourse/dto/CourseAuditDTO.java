package com.learning.lpcourse.dto;

import lombok.Data;

@Data
public class CourseAuditDTO {
    /**
     * 课程id
     */
    private Long courseId;
    /**
     * 审核状态 1通过 2驳回
     */
    private Integer auditStatus;
    /**
     * 审核备注
     */
    private String auditRemark;
}