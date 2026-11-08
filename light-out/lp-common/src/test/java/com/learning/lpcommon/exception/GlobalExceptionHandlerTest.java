package com.learning.lpcommon.exception;

import com.learning.lpcommon.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsAuthenticationFailureTo401() {
        assertError(handler.handleUnauthorized(new UnauthorizedException("未登录")), HttpStatus.UNAUTHORIZED);
        assertError(handler.handlePasswordError(new PasswordErrorException("密码错误")), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void mapsPermissionFailureTo403() {
        assertError(handler.handleForbidden(new ForbiddenException("无权访问")), HttpStatus.FORBIDDEN);
    }

    @Test
    void mapsMissingResourceTo404() {
        assertError(handler.handleNotFound(new NotFoundCourseException("课程不存在")), HttpStatus.NOT_FOUND);
    }

    @Test
    void mapsVersionConflictTo409() {
        assertError(handler.handleConflict(new ConflictException("版本冲突")), HttpStatus.CONFLICT);
    }

    @Test
    void mapsBusinessValidationTo400() {
        assertError(handler.handleBaseException(new BaseException("参数错误")), HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsUnknownFailureTo500WithoutLeakingDetails() {
        ResponseEntity<Result<Void>> response = handler.handleException(new RuntimeException("database password"));
        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals("系统繁忙，请稍后重试", response.getBody().getMsg());
    }

    private static void assertError(ResponseEntity<Result<Void>> response, HttpStatus expectedStatus) {
        assertEquals(expectedStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());
    }
}
