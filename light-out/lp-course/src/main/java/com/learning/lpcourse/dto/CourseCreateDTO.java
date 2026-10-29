package com.learning.lpcourse.dto;
import lombok.Data;

@Data
public class CourseCreateDTO {
    private String courseName;
    private String courseCode;
    private Long deptId;
    private String semester;
    private Integer isPublic;
    private String coverUrl;
    private String description;
}