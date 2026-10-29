package com.learning.learning.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteVO {
    private Long noteId;
    private Long resourceId;
    private Long startTime;
    private Long endTime;
    private String content;
    private LocalDateTime createdAt;
}
