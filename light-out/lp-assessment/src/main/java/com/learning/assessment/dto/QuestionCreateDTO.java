package com.learning.assessment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class QuestionCreateDTO {

    // Lombok 的 getQType()/getQId() 会被 Jackson 推成 qtype/qid，显式声明保持驼峰契约
    @JsonProperty("qId")
    private Long qId;

    private Long courseId;

    // 题型：single_choice-单选 / multiple_choice-多选 / judge-判断 / fill_blank-填空 / short_answer-简答
    @JsonProperty("qType")
    private String qType;

    private String content;

    // 选项列表，仅选择题使用
    private List<String> options;

    // 参考答案：选择题为选项下标序号（单选"0"，多选"0,2,3"），判断为"1"/"0"
    private String answer;

    private String analysis;

    // 难度：1-简单 2-中等 3-困难
    private Integer difficulty;

    private List<String> tags;

    // 乐观锁版本号，编辑时必填
    private Integer version;
}