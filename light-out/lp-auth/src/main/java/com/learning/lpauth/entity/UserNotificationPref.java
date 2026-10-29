package com.learning.lpauth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationPref {
    private Long id;
    private Long userId;
    private String channel; // email / sms
    private Integer enabled; // 0关闭 1开启
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
