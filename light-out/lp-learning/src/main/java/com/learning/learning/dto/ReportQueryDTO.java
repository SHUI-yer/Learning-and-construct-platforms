package com.learning.learning.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportQueryDTO {

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "课程ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @Schema(description = "报告类型：weekly-周报，monthly-月报", example = "weekly", defaultValue = "weekly")
    private String reportType = "weekly";

    @Schema(description = "统计周期", example = "week", defaultValue = "week")
    private String period = "week";
}
