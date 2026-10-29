package com.learning.lpauth.security;

import com.learning.lpcommon.exception.BaseException;

import java.nio.charset.StandardCharsets;

/**
 * 新口令强度校验。
 *
 * <p>规则：8~64 个字符、必须同时含字母与数字、不含空白、UTF-8 编码后不超过 72 字节
 * （BCrypt 对超过 72 字节的输入会截断，必须显式拒绝，否则"长口令"会静默退化）。</p>
 */
public final class PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;
    private static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new BaseException("新密码不能为空");
        }
        if (rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH) {
            throw new BaseException("密码长度需在 " + MIN_LENGTH + "~" + MAX_LENGTH + " 个字符之间");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new BaseException("密码过长（UTF-8 编码不得超过 " + MAX_BYTES + " 字节）");
        }
        if (rawPassword.chars().anyMatch(Character::isWhitespace)) {
            throw new BaseException("密码不能包含空格或制表符");
        }
        boolean hasLetter = rawPassword.chars().anyMatch(Character::isLetter);
        boolean hasDigit = rawPassword.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BaseException("密码必须同时包含字母和数字");
        }
    }
}
