package com.learning.learning.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class VideoControlDTO {

    @NotNull
    private Long resourceId;
    private Boolean noDrag;
    private Boolean noFastForward;
    private BigDecimal minPlaySpeed;
    private BigDecimal maxPlaySpeed;
}
