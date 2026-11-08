package com.learning.assessment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GradeAppealReviewDTO {

    @NotNull(message = "申诉ID不能为空")
    @Positive(message = "申诉ID必须为正数")
    private Long appealId;
    @NotBlank(message = "处理状态不能为空")
    @Pattern(regexp = "approved|rejected", message = "处理状态只能为approved或rejected")
    private String status;
    @Size(max = 2000, message = "教师回复不能超过2000个字符")
    private String teacherResponse;
    @DecimalMin(value = "0.00", message = "新分数不能为负数")
    private BigDecimal newScore;
    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;
}
