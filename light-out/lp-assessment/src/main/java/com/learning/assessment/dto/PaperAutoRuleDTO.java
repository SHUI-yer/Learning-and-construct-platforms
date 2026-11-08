package com.learning.assessment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaperAutoRuleDTO {

    // 题型：single_choice/multiple_choice/judge/fill_blank/short_answer
    // Lombok 的 getQType()/getQId() 会被 Jackson 推成 qtype/qid，显式声明保持驼峰契约
    @JsonProperty("qType")
    @NotBlank(message = "抽题规则题型不能为空")
    @Pattern(regexp = "single_choice|multiple_choice|judge|fill_blank|short_answer", message = "抽题规则题型不合法")
    private String qType;
    // 难度：1-简单 2-中等 3-困难，为空表示不限
    @Min(value = 1, message = "难度必须在1到3之间")
    @Max(value = 3, message = "难度必须在1到3之间")
    private Integer difficulty;
    @NotNull(message = "抽题数量不能为空")
    @Positive(message = "抽题数量必须大于0")
    private Integer count;
    @NotNull(message = "每题分值不能为空")
    @DecimalMin(value = "0.01", message = "每题分值必须大于0")
    private BigDecimal score;
}
