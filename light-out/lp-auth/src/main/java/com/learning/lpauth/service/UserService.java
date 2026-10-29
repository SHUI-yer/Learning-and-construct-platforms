package com.learning.lpauth.service;

import com.learning.lpauth.dto.ChangePasswordDTO;
import com.learning.lpauth.dto.LoginDTO;
import com.learning.lpauth.dto.ResetPasswordDTO;
import com.learning.lpauth.entity.UserLoginInfo;

public interface UserService {

    UserLoginInfo login(LoginDTO loginDTO);

    /**
     * 修改当前登录用户自己的密码：校验旧密码，新密码按 BCrypt 写入。
     */
    void changePassword(ChangePasswordDTO dto);

    /**
     * 管理员重置指定用户的密码：不校验旧密码，用于"存量账号必须重置"与用户遗忘密码。
     */
    void resetPassword(ResetPasswordDTO dto);
}
