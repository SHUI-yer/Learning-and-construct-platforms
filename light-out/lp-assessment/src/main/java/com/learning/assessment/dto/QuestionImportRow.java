package com.learning.assessment.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionImportRow {

    private int rowNo;
    private String qType;
    private String content;
    private List<String> options = new ArrayList<>();
    private String answer;
    private String analysis;
    private Integer difficulty = 2;
    private List<String> tags;
    private String error;

    public boolean isEmpty() {
        return (qType == null || qType.isBlank())
                && (content == null || content.isBlank())
                && (answer == null || answer.isBlank());
    }
}
