package com.learning.lpauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置他人密码（无需旧密码）。
 */
@Data
public class ResetPasswordDTO {
    /** 目标用户的 user_id */
    @NotNull(message = "目标用户ID不能为空")
    @Positive(message = "目标用户ID必须为正数")
    private Long userId;
    /** 新密码，须满足 PasswordPolicy */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度必须为8到64个字符")
    private String newPassword;
}
