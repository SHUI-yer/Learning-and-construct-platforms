package com.learning.lpcommon.exception;

/**
 * 需要重置密码后才能继续登录。
 *
 * <p>典型场景：口令哈希算法从 MD5 升级为 BCrypt 后，存量账号必须先重置密码，
 * 不能再用旧哈希直接换到令牌（否则旧哈希泄露后依然能登录）。</p>
 */
public class PasswordResetRequiredException extends BaseException {
    public PasswordResetRequiredException() {
    }

    public PasswordResetRequiredException(String msg) {
        super(msg);
    }
}
