package com.learning.assessment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GradeAppealReviewDTO {

    private Long appealId;
    private String status;
    private String teacherResponse;
    private BigDecimal newScore;
    private Integer version;
}
