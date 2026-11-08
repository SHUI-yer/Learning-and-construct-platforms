package com.learning.lpcommon.exception;

/** 未认证或登录状态已经失效。 */
public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
