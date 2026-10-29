package com.learning.assessment.vo;

import com.learning.assessment.entity.Submission;
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
public class SubmissionVO {

    private Long submissionId;
    private Long taskId;
    private String taskTitle;
    private Long studentId;
    private String submitType;
    private String content;
    private String attachmentUrls;
    private LocalDateTime submittedAt;
    private Integer isLate;
    private Integer isExcellent;
    private String status;
    private String teacherComment;
    private BigDecimal score;
    private BigDecimal maxScore;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ExamAnswerVO> answers;

    public static SubmissionVO fromEntity(Submission s) {
        return fromEntity(s, null, null, null);
    }

    public static SubmissionVO fromEntity(Submission s, String taskTitle, BigDecimal maxScore,
                                          List<ExamAnswerVO> answers) {
        return SubmissionVO.builder()
                .submissionId(s.getSubmissionId())
                .taskId(s.getTaskId())
                .taskTitle(taskTitle)
                .studentId(s.getStudentId())
                .submitType(s.getSubmitType())
                .content(s.getContent())
                .attachmentUrls(s.getAttachmentUrls())
                .submittedAt(s.getSubmittedAt())
                .isLate(s.getIsLate())
                .isExcellent(s.getIsExcellent())
                .status(s.getStatus())
                .teacherComment(s.getTeacherComment())
                .score(s.getScore())
                .maxScore(maxScore)
                .version(s.getVersion())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .answers(answers)
                .build();
    }
}
