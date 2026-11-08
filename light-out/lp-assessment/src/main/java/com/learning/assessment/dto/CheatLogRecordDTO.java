package com.learning.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CheatLogRecordDTO {

    @NotNull(message = "提交ID不能为空")
    @Positive(message = "提交ID必须为正数")
    private Long submissionId;

    @Valid
    @NotEmpty(message = "异常行为记录不能为空")
    @Size(max = 100, message = "单次最多上报100条异常行为")
    private List<CheatEvent> events;

    @Data
    public static class CheatEvent {
        @NotBlank(message = "异常行为类型不能为空")
        @Pattern(regexp = "tab_switch|copy_paste|right_click|over_time", message = "异常行为类型不合法")
        private String cheatType;
        @Size(max = 2000, message = "异常行为详情不能超过2000个字符")
        private String detail;
        private LocalDateTime occurredAt;
    }
}
