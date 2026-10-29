package com.learning.learning.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class AiQaDTO {

    @NotNull
    private Long resourceId;
    @NotBlank
    private String question;
    private Integer position;
}
