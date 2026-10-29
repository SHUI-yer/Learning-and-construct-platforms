package com.learning.lpcourse.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContentResource {
    /** 资源ID */
    private Long resourceId;
    /** 归属章节ID */
    private Long chapterId;
    /** 资源类型 video/graphic/link/pdf/ai_note */
    private String resourceType;
    /** 资源标题 */
    private String title;
    /** 资源url */
    private String url;
    /** 视频时长，单位秒 */
    private Integer duration;
    /** 图文HTML内容 */
    private String contentText;
    /** 排序 */
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}