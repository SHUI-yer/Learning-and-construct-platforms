package com.learning.learning.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentProgressVO {

    private Long studentId;
    private String studentName;
    private String avatarUrl;
    private BigDecimal videoPercent;
    private BigDecimal docPercent;
    private LocalDateTime lastWatchAt;
}
