package com.learning.assessment.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperQuestionVO {

    // Lombok 的 getQType()/getQId() 会被 Jackson 推成 qtype/qid，显式声明保持驼峰契约
    @JsonProperty("qId")
    private Long qId;
    @JsonProperty("qType")
    private String qType;
    private String content;
    private List<String> options;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private BigDecimal score;
    private Integer sortOrder;
}
