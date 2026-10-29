package com.learning.lpcourse.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Chapter {
    /** 章节ID */
    private Long chapterId;
    /** 所属课程ID */
    private Long courseId;
    /** 章节标题 */
    private String title;
    /** 排序序号 */
    private Integer sortOrder;
    /** 父章节ID，根节点null，实现三级目录 */
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}