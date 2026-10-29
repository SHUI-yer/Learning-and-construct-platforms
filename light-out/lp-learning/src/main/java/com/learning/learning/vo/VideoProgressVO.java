package com.learning.learning.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VideoProgressVO {

    private Integer lastPosition;
    private BigDecimal watchPercent;
    private Integer playCount;
    private Integer durationWatched;
}
