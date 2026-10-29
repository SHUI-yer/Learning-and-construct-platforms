package com.learning.assessment.vo;

import com.learning.assessment.entity.GradeSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeSummaryVO {

    private Long id;
    private Long courseId;
    private Long studentId;
    private BigDecimal finalScore;
    private Integer rankInCourse;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private Integer isExported;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GradeSummaryVO fromEntity(GradeSummary s) {
        return GradeSummaryVO.builder()
                .id(s.getId())
                .courseId(s.getCourseId())
                .studentId(s.getStudentId())
                .finalScore(s.getFinalScore())
                .rankInCourse(s.getRankInCourse())
                .confirmedBy(s.getConfirmedBy())
                .confirmedAt(s.getConfirmedAt())
                .isExported(s.getIsExported())
                .version(s.getVersion())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
