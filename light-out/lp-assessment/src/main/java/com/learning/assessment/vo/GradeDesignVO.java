package com.learning.assessment.vo;

import com.learning.assessment.entity.GradeScheme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeDesignVO {

    private Long schemeId;
    private Long courseId;
    private String componentType;
    private BigDecimal weight;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GradeDesignVO fromEntity(GradeScheme scheme) {
        return GradeDesignVO.builder()
                .schemeId(scheme.getSchemeId())
                .courseId(scheme.getCourseId())
                .componentType(scheme.getComponentType())
                .weight(scheme.getWeight())
                .sortOrder(scheme.getSortOrder())
                .createdAt(scheme.getCreatedAt())
                .updatedAt(scheme.getUpdatedAt())
                .build();
    }

    public static List<GradeDesignVO> fromEntityList(List<GradeScheme> list) {
        return list.stream().map(GradeDesignVO::fromEntity).toList();
    }
}