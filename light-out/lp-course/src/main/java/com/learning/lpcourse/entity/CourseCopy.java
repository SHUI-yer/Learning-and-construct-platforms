package com.learning.lpcourse.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CourseCopy {
    private Long id;
    /**
     * 源课程id
     */
    private Long sourceCourseId;
    /**
     * 目标新课程id
     */
    private Long targetCourseId;
    /**
     * 操作人id
     */
    private Long operatorId;
    /**
     * 复制时间
     */
    private LocalDateTime copiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}