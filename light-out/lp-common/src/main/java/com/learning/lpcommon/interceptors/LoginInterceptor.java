package com.learning.lpcommon.interceptors;

import com.learning.lpcommon.utils.JwtUtil;
import com.learning.lpcommon.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 登录校验拦截器：只负责"是否已登录"，不负责"能做什么"。
 *
 * <p>角色与数据归属校验必须在业务层完成——本拦截器通过只说明令牌有效，
 * 不代表该用户有权执行当前操作。</p>
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public LoginInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 预检请求(OPTIONS)不带 Authorization 头，必须放行，
        // 否则浏览器会把预检的 401 当成跨域失败，拦截所有带头的真实请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        try {
            Map<String, Object> claims = jwtUtil.parseToken(request.getHeader("Authorization"));
            UserContext.setLoginUser(claims);
            return true;
        } catch (Exception e) {
            // 不返回具体原因，避免把"签名错误/过期/格式非法"暴露给调用方
            log.debug("登录校验未通过: {} {} - {}", request.getMethod(), request.getRequestURI(), e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 线程池复用，必须清理，否则会把上一个请求的用户信息带给下一个请求
        UserContext.clear();
    }
}
