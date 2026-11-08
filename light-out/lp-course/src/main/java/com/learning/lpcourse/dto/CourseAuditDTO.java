package com.learning.lpcourse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseAuditDTO {
    /**
     * 课程id
     */
    @NotNull(message = "课程ID不能为空")
    @Positive(message = "课程ID必须为正数")
    private Long courseId;
    /**
     * 审核状态 1通过 2驳回
     */
    @NotNull(message = "审核状态不能为空")
    @Min(value = 1, message = "审核状态只能为1或2")
    @Max(value = 2, message = "审核状态只能为1或2")
    private Integer auditStatus;
    /**
     * 审核备注
     */
    @Size(max = 512, message = "审核备注不能超过512个字符")
    private String auditRemark;
}
