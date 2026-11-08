package com.learning.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class GradeDesignDTO {

    @NotNull(message = "课程ID不能为空")
    @Positive(message = "课程ID必须为正数")
    private Long courseId;

    @Valid
    @NotEmpty(message = "成绩构成不能为空")
    @Size(max = 20, message = "成绩构成不能超过20项")
    private List<SchemeItem> items;

    @Data
    public static class SchemeItem {
        @NotBlank(message = "成绩构成类型不能为空")
        @Size(max = 32, message = "成绩构成类型不能超过32个字符")
        private String componentType;
        @NotNull(message = "成绩权重不能为空")
        @DecimalMin(value = "0.01", message = "成绩权重必须大于0")
        @DecimalMax(value = "100.00", message = "成绩权重不能超过100")
        private BigDecimal weight;
        @PositiveOrZero(message = "排序序号不能为负数")
        private Integer sortOrder;
    }
}
