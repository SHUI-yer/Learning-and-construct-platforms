package com.learning.learning.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class SubtitleDTO {
    @NotNull
    private Long resourceId;
    @NotNull
    private String language;
    @NotNull
    private String url;
}
