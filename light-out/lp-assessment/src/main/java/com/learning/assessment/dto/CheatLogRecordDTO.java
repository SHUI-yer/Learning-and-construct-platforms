package com.learning.assessment.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CheatLogRecordDTO {

    private Long submissionId;

    private List<CheatEvent> events;

    @Data
    public static class CheatEvent {
        private String cheatType;
        private String detail;
        private LocalDateTime occurredAt;
    }
}
