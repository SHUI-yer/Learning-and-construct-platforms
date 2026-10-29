package com.learning.lpauth.dto;

import lombok.Data;

/**
 * 管理员重置他人密码（无需旧密码）。
 */
@Data
public class ResetPasswordDTO {
    /** 目标用户的 user_id */
    private Long userId;
    /** 新密码，须满足 PasswordPolicy */
    private String newPassword;
}
