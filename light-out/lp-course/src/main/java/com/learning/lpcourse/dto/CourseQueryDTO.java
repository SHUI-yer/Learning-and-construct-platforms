package com.learning.lpcourse.dto;

import lombok.Data;

@Data
public class CourseQueryDTO {
    private String courseName;
    private Integer status;
    private Long deptId;
    private String semester;
}