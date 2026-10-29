package com.learning.assessment.vo;

import com.learning.assessment.entity.Task;
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
public class TaskVO {

    private Long taskId;
    private Long courseId;
    private Long chapterId;
    private String title;
    private String description;
    private String taskType;
    private String tag;
    private BigDecimal weight;
    private String publishType;
    private LocalDateTime publishTime;
    private LocalDateTime deadline;
    private Integer allowLate;
    private BigDecimal latePenalty;
    private BigDecimal maxScore;
    private Integer allowResubmit;
    private String attachmentUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskVO fromEntity(Task task) {
        return TaskVO.builder()
                .taskId(task.getTaskId())
                .courseId(task.getCourseId())
                .chapterId(task.getChapterId())
                .title(task.getTitle())
                .description(task.getDescription())
                .taskType(task.getTaskType())
                .tag(task.getTag())
                .weight(task.getWeight())
                .publishType(task.getPublishType())
                .publishTime(task.getPublishTime())
                .deadline(task.getDeadline())
                .allowLate(task.getAllowLate())
                .latePenalty(task.getLatePenalty())
                .maxScore(task.getMaxScore())
                .allowResubmit(task.getAllowResubmit())
                .attachmentUrls(task.getAttachmentUrls())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    public static List<TaskVO> fromEntityList(List<Task> list) {
        return list.stream().map(TaskVO::fromEntity).toList();
    }
}