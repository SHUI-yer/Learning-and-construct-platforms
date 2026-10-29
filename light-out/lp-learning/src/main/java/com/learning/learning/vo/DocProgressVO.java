package com.learning.learning.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DocProgressVO {

    private Integer lastPosition;
    private BigDecimal readPercent;
}
