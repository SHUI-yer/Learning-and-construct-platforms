package com.learning.lpauth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
    /**
     * 角色层级 1超级管理员 2分院管理员 3任课教师 4助教 5学生
     */
    private Integer roleLevel;
}