package com.learning.assessment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SubmissionGradeDTO {

    private Long submissionId;
    private BigDecimal score;
    private String teacherComment;
    private Integer version;
}
