package com.learning.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class SubmissionExcellentDTO {

    @NotNull(message = "提交ID不能为空")
    @Positive(message = "提交ID必须为正数")
    private Long submissionId;
    @NotNull(message = "优秀范本标记不能为空")
    @Min(value = 0, message = "优秀范本标记只能为0或1")
    @Max(value = 1, message = "优秀范本标记只能为0或1")
    private Integer isExcellent;
    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;
}
