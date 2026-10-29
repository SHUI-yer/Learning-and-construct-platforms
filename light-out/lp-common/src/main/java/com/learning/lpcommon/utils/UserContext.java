package com.learning.lpcommon.utils;

import java.util.Map;

public class UserContext {

    // ThreadLocal容器：存放当前登录用户信息
    private static final ThreadLocal<Map<String, Object>> USER_THREAD_LOCAL = new ThreadLocal<>();

    public static void setLoginUser(Map<String, Object> userMap) {
        USER_THREAD_LOCAL.set(userMap);
    }

    public static Map<String, Object> getLoginUser() {
        return USER_THREAD_LOCAL.get();
    }

    // 获取登录用户userId
    public static Long getUserId() {
        Map<String, Object> map = getLoginUser();
        if (map == null || map.get("userId") == null) {
            return null;
        }
        return Long.valueOf(map.get("userId").toString());
    }

    // 获取角色roleCode
    public static String getRoleCode() {
        Map<String, Object> map = getLoginUser();
        if (map == null || map.get("roleCode") == null) {
            return null;
        }
        return map.get("roleCode").toString();
    }
    public static void clear() {
        USER_THREAD_LOCAL.remove();
    }
}