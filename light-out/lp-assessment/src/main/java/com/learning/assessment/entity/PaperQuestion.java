package com.learning.assessment.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaperQuestion {

    private Long id;
    private Long paperId;
    private Long qId;
    private BigDecimal score;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}