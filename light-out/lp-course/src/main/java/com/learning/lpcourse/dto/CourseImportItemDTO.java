package com.learning.lpcourse.dto;

import lombok.Data;

import java.util.List;

@Data
public class CourseImportItemDTO {
    private String courseName;
    private String courseCode;
    private Long deptId;
    private String semester;
    private Integer isPublic;
    private String coverUrl;
    private String description;
    private Long teacherId;
    /** 直接指定学生ID列表（二选一，优先级低于班级） */
    private List<Long> studentIdList;
    /** 行政班级ID列表，自动关联班级下全部学生（教务批量导入推荐） */
    private List<Long> classIdList;
}