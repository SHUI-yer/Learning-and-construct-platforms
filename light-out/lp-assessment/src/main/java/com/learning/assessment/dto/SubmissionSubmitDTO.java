package com.learning.assessment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class SubmissionSubmitDTO {

    @NotNull(message = "任务ID不能为空")
    @Positive(message = "任务ID必须为正数")
    private Long taskId;

    @Valid
    @NotEmpty(message = "作答内容不能为空")
    @Size(max = 500, message = "单次提交不能超过500道题")
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
    // Lombok 的 getQType()/getQId() 会被 Jackson 推成 qtype/qid，显式声明保持驼峰契约
        @JsonProperty("qId")
        @NotNull(message = "题目ID不能为空")
        @Positive(message = "题目ID必须为正数")
        private Long qId;
        @Size(max = 10000, message = "单题答案不能超过10000个字符")
        private String studentAnswer;
    }
}
