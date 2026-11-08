package com.learning.assessment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SubmissionGradeDTO {

    @NotNull(message = "提交ID不能为空")
    @Positive(message = "提交ID必须为正数")
    private Long submissionId;
    @NotNull(message = "分数不能为空")
    @DecimalMin(value = "0.00", message = "分数不能为负数")
    private BigDecimal score;
    @Size(max = 2000, message = "教师评语不能超过2000个字符")
    private String teacherComment;
    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;
}
