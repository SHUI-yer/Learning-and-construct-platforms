package com.learning.lpcourse.dto;

import com.learning.lpcommon.validation.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChapterDTO {
    @NotNull(groups = ValidationGroups.Update.class, message = "章节ID不能为空")
    @Positive(message = "章节ID必须为正数")
    private Long chapterId;
    @NotNull(message = "课程ID不能为空")
    @Positive(message = "课程ID必须为正数")
    private Long courseId;
    @NotBlank(message = "章节标题不能为空")
    @Size(max = 255, message = "章节标题不能超过255个字符")
    private String title;
    @PositiveOrZero(message = "排序序号不能为负数")
    private Integer sortOrder;
    @Positive(message = "父章节ID必须为正数")
    private Long parentId;
}
