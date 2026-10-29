package com.learning.learning.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class NoteDTO {
    @NotNull
    private Long resourceId;

    @NotNull
    private Long startTime;

    @NotNull
    private Long endTime;

    private String content;
}
