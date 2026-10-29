package com.learning.lpcourse.dto;

import lombok.Data;

@Data
public class ChapterSortDTO {
    /** 需要移动的章节id */
    private Long chapterId;
    /** 目标sortOrder序号 */
    private Integer targetSortOrder;
}