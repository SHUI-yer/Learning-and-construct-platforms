package com.learning.assessment.vo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.assessment.entity.QuestionImport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionImportVO {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Long id;
    private Long courseId;
    private Long operatorId;
    private String fileUrl;
    private String fileFormat;
    private Integer importCount;
    private Integer successCount;
    private Integer failCount;
    private List<ImportErrorVO> errors;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static QuestionImportVO fromEntity(QuestionImport q) {
        return fromEntity(q, true);
    }

    public static QuestionImportVO fromEntity(QuestionImport q, boolean withErrors) {
        return QuestionImportVO.builder()
                .id(q.getId())
                .courseId(q.getCourseId())
                .operatorId(q.getOperatorId())
                .fileUrl(q.getFileUrl())
                .fileFormat(q.getFileFormat())
                .importCount(q.getImportCount())
                .successCount(q.getSuccessCount())
                .failCount(q.getFailCount())
                .errors(withErrors ? parseErrors(q.getErrorDetail()) : null)
                .status(q.getStatus())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }

    private static List<ImportErrorVO> parseErrors(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<ImportErrorVO>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
