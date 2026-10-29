package com.learning.lpcourse.dto;

import lombok.Data;

@Data
public class ContentResourceDTO {
    private Long resourceId;
    private Long chapterId;
    private String resourceType;
    private String title;
    private String url;
    private Integer duration;
    private String contentText;
    private Integer sortOrder;
}