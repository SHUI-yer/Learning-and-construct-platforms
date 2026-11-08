package com.learning.assessment.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaperPublishDTO {

    @NotNull(message = "试卷ID不能为空")
    @Positive(message = "试卷ID必须为正数")
    private Long paperId;
    @NotNull(message = "课程ID不能为空")
    @Positive(message = "课程ID必须为正数")
    private Long courseId;
    @Size(max = 255, message = "任务标题不能超过255个字符")
    private String title;
    @Size(max = 5000, message = "任务描述不能超过5000个字符")
    private String description;
    // 任务类型：quiz-测验/考试 assignment-作业
    @NotBlank(message = "任务类型不能为空")
    @Pattern(regexp = "quiz|assignment", message = "任务类型只能为quiz或assignment")
    private String taskType;
    @Size(max = 64, message = "任务标签不能超过64个字符")
    private String tag;
    @DecimalMin(value = "0.00", message = "任务权重不能小于0")
    @DecimalMax(value = "100.00", message = "任务权重不能超过100")
    private BigDecimal weight;
    @NotBlank(message = "发布方式不能为空")
    @Pattern(regexp = "immediate|scheduled", message = "发布方式只能为immediate或scheduled")
    private String publishType;
    private LocalDateTime publishTime;
    @NotNull(message = "截止时间不能为空")
    private LocalDateTime deadline;
    @Min(value = 0, message = "是否允许迟交只能为0或1")
    @Max(value = 1, message = "是否允许迟交只能为0或1")
    private Integer allowLate;
    @DecimalMin(value = "0.00", message = "迟交扣分比例不能小于0")
    @DecimalMax(value = "100.00", message = "迟交扣分比例不能超过100")
    private BigDecimal latePenalty;
    @DecimalMin(value = "0.01", message = "满分必须大于0")
    private BigDecimal maxScore;
    @Min(value = 0, message = "是否允许重交只能为0或1")
    @Max(value = 1, message = "是否允许重交只能为0或1")
    private Integer allowResubmit;
}
