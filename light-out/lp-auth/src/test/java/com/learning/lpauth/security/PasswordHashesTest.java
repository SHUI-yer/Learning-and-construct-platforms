package com.learning.lpauth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 口令哈希策略的行为约束：BCrypt 用于新口令，存量 MD5 可被识别并在迁移后失效。
 */
class PasswordHashesTest {

    /** 明文 123456 的无盐 MD5，即改造前数据库里的存储形态 */
    private static final String LEGACY_MD5_123456 = "e10adc3949ba59abbe56e057f20f883e";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @Test
    @DisplayName("BCrypt 哈希被正确识别，且不会被误判为存量 MD5")
    void recognizesBcrypt() {
        String hash = encoder.encode("Passw0rd!中文");

        assertTrue(PasswordHashes.isBcrypt(hash));
        assertFalse(PasswordHashes.isLegacyMd5(hash));
        assertTrue(encoder.matches("Passw0rd!中文", hash));
        assertFalse(encoder.matches("Passw0rd!", hash));
    }

    @Test
    @DisplayName("BCrypt 每次编码结果不同（含随机盐），但都能校验通过")
    void bcryptUsesRandomSalt() {
        String first = encoder.encode("123456");
        String second = encoder.encode("123456");

        assertNotEquals(first, second);
        assertTrue(encoder.matches("123456", first));
        assertTrue(encoder.matches("123456", second));
    }

    @Test
    @DisplayName("32 位十六进制被识别为存量 MD5，大小写均可")
    void recognizesLegacyMd5() {
        assertTrue(PasswordHashes.isLegacyMd5(LEGACY_MD5_123456));
        assertTrue(PasswordHashes.isLegacyMd5(LEGACY_MD5_123456.toUpperCase()));
        assertFalse(PasswordHashes.isLegacyMd5(LEGACY_MD5_123456.substring(1)));
        assertFalse(PasswordHashes.isLegacyMd5("123456"));
    }

    @Test
    @DisplayName("存量 MD5 校验：正确明文通过，错误明文不通过")
    void verifiesLegacyMd5() {
        assertEquals("e10adc3949ba59abbe56e057f20f883e", PasswordHashes.md5Hex("123456"));

        assertTrue(PasswordHashes.md5Matches("123456", LEGACY_MD5_123456));
        assertTrue(PasswordHashes.md5Matches("123456", LEGACY_MD5_123456.toUpperCase()));
        assertFalse(PasswordHashes.md5Matches("654321", LEGACY_MD5_123456));
        assertFalse(PasswordHashes.md5Matches("", LEGACY_MD5_123456));
    }

    @Test
    @DisplayName("迁移后（MD5 -> BCrypt）原口令仍可登录，旧哈希不再可用")
    void migratedHashStillWorks() {
        // 先按存量方式校验通过，再迁移为 BCrypt
        assertTrue(PasswordHashes.md5Matches("123456", LEGACY_MD5_123456));
        String migrated = encoder.encode("123456");

        assertTrue(PasswordHashes.isBcrypt(migrated));
        assertFalse(PasswordHashes.isLegacyMd5(migrated));
        assertTrue(encoder.matches("123456", migrated));
        assertFalse(encoder.matches("123457", migrated));
    }

    @Test
    @DisplayName("无法识别的哈希一律拒绝，不会被当成通过条件")
    void rejectsUnknownFormat() {
        assertFalse(PasswordHashes.isBcrypt("hello"));
        assertFalse(PasswordHashes.isLegacyMd5("hello"));
        assertFalse(PasswordHashes.isBcrypt("$2a$10$too-short"));
        assertFalse(PasswordHashes.md5Matches("123456", "hello"));
    }

    @Test
    @DisplayName("null 输入不抛异常")
    void handlesNullSafely() {
        assertFalse(PasswordHashes.isBcrypt(null));
        assertFalse(PasswordHashes.isLegacyMd5(null));
        assertFalse(PasswordHashes.md5Matches(null, LEGACY_MD5_123456));
        assertFalse(PasswordHashes.md5Matches("123456", null));
    }
}
