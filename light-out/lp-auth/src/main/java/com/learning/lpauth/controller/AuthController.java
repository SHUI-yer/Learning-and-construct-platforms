package com.learning.lpauth.controller;

import com.learning.lpauth.dto.LoginDTO;
import com.learning.lpauth.entity.Role;
import com.learning.lpauth.entity.User;
import com.learning.lpauth.entity.UserLoginInfo;
import com.learning.lpauth.service.UserService;
import com.learning.lpauth.vo.LoginVO;
import com.learning.lpcommon.result.Result;
import com.learning.lpcommon.utils.JwtUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        UserLoginInfo userLoginInfo = userService.login(loginDTO);
        User user = userLoginInfo.getUser();
        Role role = userLoginInfo.getRole();
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("roleCode", role.getRoleCode());
        String token = jwtUtil.createToken(claims);
        LoginVO loginVO = LoginVO.builder()
                .token(token)
                .userId(user.getUserId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roleCode(role.getRoleCode())
                .roleLevel(role.getLevel())
                .build();
        return Result.success(loginVO);
    }
}
