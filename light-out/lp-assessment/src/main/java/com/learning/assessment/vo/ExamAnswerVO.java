package com.learning.assessment.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ExamAnswerVO {

    // Lombok 的 getQType()/getQId() 会被 Jackson 推成 qtype/qid，显式声明保持驼峰契约
    @JsonProperty("qId")
    private Long qId;
    @JsonProperty("qType")
    private String qType;
    private String content;
    private List<String> options;
    private String answer;
    private String analysis;
    private String studentAnswer;
    private Integer isCorrect;
    private BigDecimal score;
    private BigDecimal fullScore;
    private Integer sortOrder;
}
