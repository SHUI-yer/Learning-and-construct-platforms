package com.learning.lpauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改自己的密码（需要提供当前密码）。
 */
@Data
public class ChangePasswordDTO {
    /** 当前密码，用于确认是本人操作 */
    @NotBlank(message = "当前密码不能为空")
    @Size(max = 128, message = "当前密码不能超过128个字符")
    private String oldPassword;
    /** 新密码，须满足 PasswordPolicy */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度必须为8到64个字符")
    private String newPassword;
}
