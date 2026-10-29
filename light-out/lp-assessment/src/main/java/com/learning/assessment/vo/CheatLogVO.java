package com.learning.assessment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheatLogVO {

    private Long id;
    private Long submissionId;
    private String cheatType;
    private String cheatTypeName;
    private String detail;
    private LocalDateTime occurredAt;
}
