package com.learning.lpauth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * 存量口令哈希的识别与校验。
 *
 * <p>历史数据里的密码是无盐 MD5（32 位十六进制）。该算法已不再用于写入，
 * 这里的代码只服务于一件事：让存量账号在下次登录时能被验证出来，并立刻迁移成 BCrypt。</p>
 */
public final class PasswordHashes {

    /** BCrypt 固定格式：$2a$/$2b$/$2y$ + cost + 53 字符盐与摘要 */
    private static final String BCRYPT_PATTERN = "^\\$2[aby]\\$\\d{2}\\$.{53}$";

    /** 无盐 MD5：32 位十六进制 */
    private static final String LEGACY_MD5_PATTERN = "^[0-9a-fA-F]{32}$";

    private PasswordHashes() {
    }

    public static boolean isBcrypt(String hash) {
        return hash != null && hash.matches(BCRYPT_PATTERN);
    }

    public static boolean isLegacyMd5(String hash) {
        return hash != null && hash.matches(LEGACY_MD5_PATTERN);
    }

    /**
     * 用 MD5 校验明文口令（仅用于存量账号迁移）。
     *
     * <p>使用 {@link MessageDigest#isEqual} 做常量时间比较，避免逐字节比较带来的
     * 时序侧信道。</p>
     */
    public static boolean md5Matches(String rawPassword, String storedHash) {
        if (rawPassword == null || rawPassword.isEmpty() || !isLegacyMd5(storedHash)) {
            return false;
        }
        byte[] expected = storedHash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = md5Hex(rawPassword).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }

    /**
     * 计算 MD5 十六进制串。
     *
     * <p>历史哈希是按 JVM 默认字符集对明文取摘要生成的；在 UTF-8 为默认字符集的
     * JVM（Java 18+）上与这里一致。</p>
     */
    static String md5Hex(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] sum = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(sum.length * 2);
            for (byte b : sum) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 MD5，无法校验存量口令", e);
        }
    }
}
