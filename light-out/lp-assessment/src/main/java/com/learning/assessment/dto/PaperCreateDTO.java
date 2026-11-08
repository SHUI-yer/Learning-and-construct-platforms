package com.learning.assessment.dto;

import com.learning.lpcommon.validation.ValidationGroups;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PaperCreateDTO {

    @NotNull(groups = ValidationGroups.Update.class, message = "试卷ID不能为空")
    @Positive(message = "试卷ID必须为正数")
    private Long paperId;
    @NotNull(message = "课程ID不能为空")
    @Positive(message = "课程ID必须为正数")
    private Long courseId;
    @NotBlank(message = "试卷标题不能为空")
    @Size(max = 255, message = "试卷标题不能超过255个字符")
    private String title;
    // 限时(分钟)，可为空
    @Positive(message = "考试时长必须大于0")
    @Max(value = 600, message = "考试时长不能超过600分钟")
    private Integer duration;
    // 是否乱序出题: 1-是 0-否
    @Min(value = 0, message = "题目乱序只能为0或1")
    @Max(value = 1, message = "题目乱序只能为0或1")
    private Integer shuffleQuestions;
    // 乐观锁版本号，更新时必填
    @NotNull(groups = ValidationGroups.Update.class, message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;
    // 题目项（qId + score），为空则不重建题目
    @Valid
    @Size(max = 500, message = "单张试卷不能超过500道题")
    private List<PaperQuestionItemDTO> items;
}
