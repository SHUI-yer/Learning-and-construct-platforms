package com.learning.lpcourse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CourseTeacher {
    private Long id;
    private Long courseId;
    private Long teacherId;
    private String role;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}