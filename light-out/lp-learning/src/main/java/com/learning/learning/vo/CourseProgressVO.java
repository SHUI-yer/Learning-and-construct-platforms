package com.learning.learning.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseProgressVO {

    private BigDecimal videoPercent;
    private BigDecimal docPercent;
    private Integer totalDuration;
    private Integer tasksCompleted;
}
