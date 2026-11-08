package com.learning.assessment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaperQuestionItemDTO {

    // Lombok 的 getQType()/getQId() 会被 Jackson 推成 qtype/qid，显式声明保持驼峰契约
    @JsonProperty("qId")
    @NotNull(message = "题目ID不能为空")
    @Positive(message = "题目ID必须为正数")
    private Long qId;
    @NotNull(message = "题目分值不能为空")
    @DecimalMin(value = "0.01", message = "题目分值必须大于0")
    private BigDecimal score;
}
