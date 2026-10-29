package com.learning.lpcourse.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CourseVO {
    private Long courseId;
    private String courseName;
    private String courseCode;
    private Long teacherId;
    private Long deptId;
    private Integer status;
    private String semester;
    private Integer isPublic;
    private String coverUrl;
    private String description;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}