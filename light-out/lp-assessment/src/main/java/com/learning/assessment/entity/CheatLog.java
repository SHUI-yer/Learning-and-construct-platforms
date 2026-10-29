package com.learning.assessment.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CheatLog {

    private Long id;
    private Long submissionId;
    private String cheatType;
    private String detail;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}