package com.learning.lpcommon.interceptors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.lpcommon.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginInterceptorTest {

    @Test
    void rejectedRequestReturnsJsonResultAnd401() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(jwtUtil.parseToken(any())).thenThrow(new IllegalArgumentException("invalid token"));
        LoginInterceptor interceptor = new LoginInterceptor(jwtUtil, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/course/page");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEquals(0, new ObjectMapper().readTree(response.getContentAsString()).get("code").asInt());
    }
}
