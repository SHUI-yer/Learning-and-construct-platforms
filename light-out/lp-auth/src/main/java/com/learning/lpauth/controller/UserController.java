package com.learning.lpauth.controller;

import com.learning.lpauth.dto.ChangePasswordDTO;
import com.learning.lpauth.dto.ResetPasswordDTO;
import com.learning.lpauth.service.UserService;
import com.learning.lpcommon.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 口令管理接口。
 *
 * <p>两个接口都在登录拦截器的保护范围内（仅 {@code /login} 被排除），
 * 因此调用方必须已登录；"管理员重置"还会在服务层校验角色。</p>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 修改自己的密码：需要提供当前密码，新密码须满足强度要求。
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return Result.success();
    }

    /**
     * 管理员重置他人密码：用于存量账号强制重置、用户遗忘密码。
     * 重置后该账号所有已签发的令牌仍会在过期前有效（JWT 无状态），必要时请配合下线处理。
     */
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(dto);
        return Result.success();
    }
}
