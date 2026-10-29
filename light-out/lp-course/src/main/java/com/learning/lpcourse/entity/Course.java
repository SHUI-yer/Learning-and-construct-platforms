package com.learning.lpcourse.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Course {
    /**
     * 课程ID
     */
    private Long courseId;
    /**
     * 课程名称
     */
    private String courseName;
    /**
     * 课程编码
     */
    private String courseCode;
    /**
     * 创建教师ID
     */
    private Long teacherId;
    /**
     * 所属分院ID
     */
    private Long deptId;
    /**
     * 状态: 0-草稿 1-待审核 2-已上线 3-已归档
     */
    private Integer status;
    /**
     * 学期
     */
    private String semester;
    /**
     * 是否公开课 1是 0否
     */
    private Integer isPublic;
    /**
     * 封面URL
     */
    private String coverUrl;
    /**
     * 课程简介
     */
    private String description;
    /**
     * 软删除时间
     */
    private LocalDateTime deletedAt;
    /**
     * 乐观锁版本号
     */
    private Integer version;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}