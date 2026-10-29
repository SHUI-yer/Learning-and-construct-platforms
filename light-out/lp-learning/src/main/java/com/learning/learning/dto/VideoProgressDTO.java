package com.learning.learning.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class VideoProgressDTO {

    @NotNull
    private Long resourceId;
    private Integer currentPosition;
    private Integer totalDuration;
}
