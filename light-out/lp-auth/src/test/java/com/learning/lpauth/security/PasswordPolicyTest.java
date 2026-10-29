package com.learning.lpauth.security;

import com.learning.lpcommon.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 新口令强度策略的边界约束。
 */
class PasswordPolicyTest {

    @Test
    @DisplayName("合规密码通过：8 位以上、同时含字母与数字")
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Abcd1234"));
        assertDoesNotThrow(() -> PasswordPolicy.validate("Student2026Pass"));
    }

    @Test
    @DisplayName("空密码被拒绝")
    void rejectsEmpty() {
        assertThrows(BaseException.class, () -> PasswordPolicy.validate(null));
        assertThrows(BaseException.class, () -> PasswordPolicy.validate(""));
    }

    @Test
    @DisplayName("长度越界被拒绝")
    void rejectsBadLength() {
        assertThrows(BaseException.class, () -> PasswordPolicy.validate("Ab1"));          // 3 位，过短
        assertDoesNotThrow(() -> PasswordPolicy.validate("Abc12345"));                    // 8 位，正好是下限
        assertThrows(BaseException.class, () -> PasswordPolicy.validate("A1" + "b2".repeat(32))); // 66 位，过长
    }

    @Test
    @DisplayName("缺少字母或数字被拒绝")
    void requiresBothLetterAndDigit() {
        assertThrows(BaseException.class, () -> PasswordPolicy.validate("abcdefgh"));
        assertThrows(BaseException.class, () -> PasswordPolicy.validate("12345678"));
    }

    @Test
    @DisplayName("含空白字符被拒绝")
    void rejectsWhitespace() {
        assertThrows(BaseException.class, () -> PasswordPolicy.validate("Abcd 1234"));
        assertThrows(BaseException.class, () -> PasswordPolicy.validate("Abcd\t1234"));
    }

    @Test
    @DisplayName("超过 BCrypt 72 字节上限被拒绝（避免长口令被静默截断）")
    void rejectsOverlongUtf8Bytes() {
        // 24 个汉字 = 72 字节，再加数字会超过限制；字符数仍在 64 以内
        String overBytes = "中文".repeat(23) + "1";
        assertThrows(BaseException.class, () -> PasswordPolicy.validate(overBytes));
    }
}
