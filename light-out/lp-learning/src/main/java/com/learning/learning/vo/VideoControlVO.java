package com.learning.learning.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VideoControlVO {

    private Boolean noDrag;
    private Boolean noFastForward;
    private BigDecimal minPlaySpeed;
    private BigDecimal maxPlaySpeed;
}
