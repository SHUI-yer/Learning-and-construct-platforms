package com.learning.lpcourse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentClass {
    private Long id;
    private Long classId;
    private Long studentId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}