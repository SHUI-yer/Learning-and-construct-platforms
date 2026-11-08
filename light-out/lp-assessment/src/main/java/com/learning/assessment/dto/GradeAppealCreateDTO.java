package com.learning.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GradeAppealCreateDTO {

    @NotNull(message = "成绩记录ID不能为空")
    @Positive(message = "成绩记录ID必须为正数")
    private Long gradeId;
    @NotBlank(message = "申诉理由不能为空")
    @Size(max = 2000, message = "申诉理由不能超过2000个字符")
    private String appealReason;
}
