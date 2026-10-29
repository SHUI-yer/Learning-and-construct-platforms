package com.learning.lpauth.dto;

import lombok.Data;

/**
 * 修改自己的密码（需要提供当前密码）。
 */
@Data
public class ChangePasswordDTO {
    /** 当前密码，用于确认是本人操作 */
    private String oldPassword;
    /** 新密码，须满足 PasswordPolicy */
    private String newPassword;
}
