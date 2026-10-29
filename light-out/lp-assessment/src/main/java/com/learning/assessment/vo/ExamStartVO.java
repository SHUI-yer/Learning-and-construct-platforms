package com.learning.assessment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamStartVO {

    private Long submissionId;
    private Long taskId;
    private LocalDateTime startedAt;
    private Integer duration;
    private LocalDateTime deadline;
}
