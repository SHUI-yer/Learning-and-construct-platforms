package com.learning.learning.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class VideoQualityDTO {
    @NotNull
    private Long resourceId;
    @NotNull
    private String label;
    @NotNull
    private String resolution;
    @NotNull
    private String url;
}
