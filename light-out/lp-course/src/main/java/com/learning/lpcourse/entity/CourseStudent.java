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
public class CourseStudent {
    private Long id;
    private Long course;
    private Long courseId;
    private Long studentId;
    private String joinType;
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;
    private Integer version;
    private LocalDateTime updatedAt;
}