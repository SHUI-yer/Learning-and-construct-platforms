package com.learning.assessment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaperAutoRuleDTO {

    // 题型：single_choice/multiple_choice/judge/fill_blank/short_answer
    // Lombok 的 getQType()/getQId() 会被 Jackson 推成 qtype/qid，显式声明保持驼峰契约
    @JsonProperty("qType")
    private String qType;
    // 难度：1-简单 2-中等 3-困难，为空表示不限
    private Integer difficulty;
    private Integer count;
    private BigDecimal score;
}
