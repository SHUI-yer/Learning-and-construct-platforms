package com.learning.assessment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learning.lpcommon.validation.ValidationGroups;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class QuestionCreateDTO {

    // Lombok 的 getQType()/getQId() 会被 Jackson 推成 qtype/qid，显式声明保持驼峰契约
    @JsonProperty("qId")
    @NotNull(groups = ValidationGroups.Update.class, message = "题目ID不能为空")
    @Positive(message = "题目ID必须为正数")
    private Long qId;

    @NotNull(message = "课程ID不能为空")
    @Positive(message = "课程ID必须为正数")
    private Long courseId;

    // 题型：single_choice-单选 / multiple_choice-多选 / judge-判断 / fill_blank-填空 / short_answer-简答
    @JsonProperty("qType")
    @NotBlank(message = "题型不能为空")
    @Pattern(regexp = "single_choice|multiple_choice|judge|fill_blank|short_answer", message = "题型不合法")
    private String qType;

    @NotBlank(message = "题干内容不能为空")
    @Size(max = 10000, message = "题干内容不能超过10000个字符")
    private String content;

    // 选项列表，仅选择题使用
    @Size(max = 20, message = "题目选项不能超过20个")
    private List<@NotBlank(message = "选项内容不能为空") @Size(max = 2000, message = "单个选项不能超过2000个字符") String> options;

    // 参考答案：选择题为选项下标序号（单选"0"，多选"0,2,3"），判断为"1"/"0"
    @NotBlank(message = "参考答案不能为空")
    @Size(max = 10000, message = "参考答案不能超过10000个字符")
    private String answer;

    @Size(max = 10000, message = "答案解析不能超过10000个字符")
    private String analysis;

    // 难度：1-简单 2-中等 3-困难
    @Min(value = 1, message = "难度必须在1到3之间")
    @Max(value = 3, message = "难度必须在1到3之间")
    private Integer difficulty;

    @Size(max = 20, message = "题目标签不能超过20个")
    private List<@NotBlank(message = "标签不能为空") @Size(max = 64, message = "单个标签不能超过64个字符") String> tags;

    // 乐观锁版本号，编辑时必填
    @NotNull(groups = ValidationGroups.Update.class, message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;
}
