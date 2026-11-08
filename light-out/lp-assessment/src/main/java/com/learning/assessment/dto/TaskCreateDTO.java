package com.learning.assessment.dto;

import com.learning.lpcommon.validation.ValidationGroups;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TaskCreateDTO {

    @NotNull(groups = ValidationGroups.Update.class, message = "任务ID不能为空")
    @Positive(message = "任务ID必须为正数")
    private Long taskId;
    @NotNull(message = "课程ID不能为空")
    @Positive(message = "课程ID必须为正数")
    private Long courseId;
    @Positive(message = "章节ID必须为正数")
    private Long chapterId;
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 255, message = "任务标题不能超过255个字符")
    private String title;
    @Size(max = 5000, message = "任务描述不能超过5000个字符")
    private String description;
    @NotBlank(message = "任务类型不能为空")
    @Pattern(regexp = "assignment|checkin|quiz|experiment", message = "任务类型不合法")
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
    @Size(max = 5000, message = "附件地址内容过长")
    private String attachmentUrls;
}
