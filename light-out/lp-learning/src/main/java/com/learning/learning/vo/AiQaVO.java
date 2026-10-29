package com.learning.learning.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiQaVO {

    private String question;
    private String answer;
    private List<String> sourceRefs;
    private LocalDateTime askedAt;
}
