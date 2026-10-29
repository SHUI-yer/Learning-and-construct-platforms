package com.learning.assessment.vo;

import com.learning.assessment.entity.ExamPaper;
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
public class ExamPaperVO {

    private Long paperId;
    private Long courseId;
    private Long taskId;
    private String title;
    private BigDecimal totalScore;
    private Integer duration;
    private Integer shuffleQuestions;
    private Integer version;
    private List<PaperQuestionVO> questions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ExamPaperVO fromEntity(ExamPaper p) {
        return fromEntity(p, null);
    }

    public static ExamPaperVO fromEntity(ExamPaper p, List<PaperQuestionVO> questions) {
        return ExamPaperVO.builder()
                .paperId(p.getPaperId())
                .courseId(p.getCourseId())
                .taskId(p.getTaskId())
                .title(p.getTitle())
                .totalScore(p.getTotalScore())
                .duration(p.getDuration())
                .shuffleQuestions(p.getShuffleQuestions())
                .version(p.getVersion())
                .questions(questions)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
