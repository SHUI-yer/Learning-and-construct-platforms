package com.learning.lpcourse.dto;

import lombok.Data;

@Data
public class ChapterDTO {
    private Long chapterId;
    private Long courseId;
    private String title;
    private Integer sortOrder;
    private Long parentId;
}