package com.learning.assessment.vo;

import com.learning.assessment.entity.Grade;
import com.learning.assessment.entity.GradeAppeal;
import com.learning.assessment.entity.Task;
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
public class GradeAppealVO {

    private Long appealId;
    private Long gradeId;
    private Long courseId;
    private Long taskId;
    private String taskTitle;
    private Long studentId;
    private BigDecimal currentScore;
    private String appealReason;
    private String teacherResponse;
    private BigDecimal newScore;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private Integer version;
    private LocalDateTime updatedAt;

    public static GradeAppealVO fromEntity(GradeAppeal a, Grade grade, Task task) {
        return GradeAppealVO.builder()
                .appealId(a.getAppealId())
                .gradeId(a.getGradeId())
                .courseId(grade == null ? null : grade.getCourseId())
                .taskId(grade == null ? null : grade.getTaskId())
                .taskTitle(task == null ? null : task.getTitle())
                .studentId(a.getStudentId())
                .currentScore(grade == null ? null : grade.getScore())
                .appealReason(a.getAppealReason())
                .teacherResponse(a.getTeacherResponse())
                .newScore(a.getNewScore())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .resolvedAt(a.getResolvedAt())
                .version(a.getVersion())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
