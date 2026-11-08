package com.learning.lpcourse.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ChapterSortDTO {
    /** 需要移动的章节id */
    @NotNull(message = "章节ID不能为空")
    @Positive(message = "章节ID必须为正数")
    private Long chapterId;
    /** 目标sortOrder序号 */
    @NotNull(message = "目标排序序号不能为空")
    @PositiveOrZero(message = "目标排序序号不能为负数")
    private Integer targetSortOrder;
}
