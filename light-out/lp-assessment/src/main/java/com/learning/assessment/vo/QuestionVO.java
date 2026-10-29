package com.learning.assessment.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learning.assessment.entity.QuestionBank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionVO {

    // Lombok 的 getQType()/getQId() 会被 Jackson 推成 qtype/qid，显式声明保持驼峰契约
    @JsonProperty("qId")
    private Long qId;
    private Long courseId;
    private Long creatorId;

    // 题型：single_choice-单选 / multiple_choice-多选 / judge-判断 / fill_blank-填空 / short_answer-简答
    @JsonProperty("qType")
    private String qType;

    private String content;

    private List<String> options;

    private String answer;

    private String analysis;

    // 难度：1-简单 2-中等 3-困难
    private Integer difficulty;

    private List<String> tags;

    // 来源：manual-手动 / batch_import-批量导入 / ai_generated-AI生成
    private String source;

    // 乐观锁版本号，编辑题目时必须回传（QuestionBankServiceImpl.updateQuestion 校验）
    private Integer version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static QuestionVO fromEntity(QuestionBank q) {
        return QuestionVO.builder()
                .qId(q.getQId())
                .courseId(q.getCourseId())
                .creatorId(q.getCreatorId())
                .qType(q.getQType())
                .content(q.getContent())
                .options(parseJsonList(q.getOptions()))
                .answer(q.getAnswer())
                .analysis(q.getAnalysis())
                .difficulty(q.getDifficulty())
                .tags(parseJsonList(q.getTags()))
                .source(q.getSource())
                .version(q.getVersion())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }

    private static List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String inner = trimmed.substring(1, trimmed.length() - 1);
            if (inner.isBlank()) {
                return Collections.emptyList();
            }
            return Arrays.stream(inner.split(","))
                    .map(String::trim)
                    .map(s -> s.replaceAll("^\"|\"$", ""))
                    .toList();
        }
        return Collections.emptyList();
    }
}