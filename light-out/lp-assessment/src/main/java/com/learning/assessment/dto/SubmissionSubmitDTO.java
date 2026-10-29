package com.learning.assessment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class SubmissionSubmitDTO {

    private Long taskId;

    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
    // Lombok 的 getQType()/getQId() 会被 Jackson 推成 qtype/qid，显式声明保持驼峰契约
        @JsonProperty("qId")
        private Long qId;
        private String studentAnswer;
    }
}
