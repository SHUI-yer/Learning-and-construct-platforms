package com.learning.lpauth.entity;

import lombok.Data;

@Data
public class UserLoginInfo {
    private User user;
    private Role role;
}
