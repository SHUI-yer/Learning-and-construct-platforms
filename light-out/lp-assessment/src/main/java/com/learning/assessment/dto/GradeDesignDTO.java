package com.learning.assessment.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class GradeDesignDTO {

    private Long courseId;

    private List<SchemeItem> items;

    @Data
    public static class SchemeItem {
        private String componentType;
        private BigDecimal weight;
        private Integer sortOrder;
    }
}