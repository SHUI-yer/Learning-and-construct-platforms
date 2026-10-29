package com.learning.lpcourse.dto;

import lombok.Data;

@Data
public class CourseCopyDTO {
    /**
     * 源课程ID
     */
    private Long sourceCourseId;
    // 可覆盖的新课程信息
    private String courseName;
    private String courseCode;
    private Long deptId;
    private String semester;
    private Integer isPublic;
    private String coverUrl;
    private String description;

    /**
     * mybatis回填：复制生成的新课程ID
     */
    private Long targetCourseId;
}