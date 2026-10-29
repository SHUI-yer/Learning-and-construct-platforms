package com.learning.lpcommon.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT 签发与校验的行为约束。
 *
 * <p>重点覆盖两件安全属性：密钥必须来自外部配置且足够强；任何非法令牌都必须被拒绝。</p>
 */
class JwtUtilTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String ISSUER = "learning-platform";

    private static JwtUtil util() {
        return new JwtUtil(SECRET, 12);
    }

    private static Map<String, Object> claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 42L);
        claims.put("roleCode", "teacher");
        return claims;
    }

    @Test
    @DisplayName("签发的令牌可被解析，业务载荷原样返回")
    void roundTrip() {
        JwtUtil jwt = util();
        String token = jwt.createToken(claims());

        Map<String, Object> parsed = jwt.parseToken("Bearer " + token);

        assertNotNull(parsed);
        assertEquals("42", String.valueOf(parsed.get("userId")));
        assertEquals("teacher", parsed.get("roleCode"));
    }

    @Test
    @DisplayName("裸 token（不带 Bearer 前缀）同样可解析")
    void acceptsBareToken() {
        JwtUtil jwt = util();
        String token = jwt.createToken(claims());

        assertNotNull(jwt.parseToken(token));
    }

    @Test
    @DisplayName("密钥被替换后，旧令牌一律失效")
    void rejectsTokenSignedByAnotherSecret() {
        String token = util().createToken(claims());
        JwtUtil other = new JwtUtil("ffffffffffffffffffffffffffffffffffffffff", 12);

        assertThrows(RuntimeException.class, () -> other.parseToken(token));
    }

    @Test
    @DisplayName("被篡改的令牌被拒绝")
    void rejectsTamperedToken() {
        JwtUtil jwt = util();
        String token = jwt.createToken(claims());
        String tampered = token.substring(0, token.length() - 2) + "xy";

        assertThrows(RuntimeException.class, () -> jwt.parseToken(tampered));
    }

    @Test
    @DisplayName("缺少 issuer 或已过期的令牌被拒绝")
    void rejectsForeignAndExpiredTokens() {
        JwtUtil jwt = util();

        String noIssuer = JWT.create()
                .withClaim("claims", claims())
                .withExpiresAt(new Date(System.currentTimeMillis() + 600_000))
                .sign(Algorithm.HMAC256(SECRET));
        assertThrows(RuntimeException.class, () -> jwt.parseToken(noIssuer));

        String expired = JWT.create()
                .withIssuer(ISSUER)
                .withClaim("claims", claims())
                .withExpiresAt(new Date(System.currentTimeMillis() - 60_000))
                .sign(Algorithm.HMAC256(SECRET));
        assertThrows(RuntimeException.class, () -> jwt.parseToken(expired));
    }

    @Test
    @DisplayName("空 Authorization 头被拒绝")
    void rejectsEmptyHeader() {
        JwtUtil jwt = util();

        assertThrows(RuntimeException.class, () -> jwt.parseToken(null));
        assertThrows(RuntimeException.class, () -> jwt.parseToken("   "));
    }

    @Test
    @DisplayName("弱密钥让应用启动失败（不再有硬编码 2 字符密钥的退路）")
    void rejectsWeakSecretAtStartup() {
        // 改造前源码里写的是 "qz"，这里确保这种密钥再也无法通过
        assertThrows(IllegalStateException.class, () -> new JwtUtil("qz", 12));
        assertThrows(IllegalStateException.class, () -> new JwtUtil("a".repeat(31), 12));
        assertThrows(IllegalStateException.class, () -> new JwtUtil("", 12));
        assertThrows(IllegalStateException.class, () -> new JwtUtil(null, 12));
        // 32 字节刚好达到下限，应当可用
        assertNotNull(new JwtUtil("a".repeat(32), 12));
    }

    @Test
    @DisplayName("非法有效期被拒绝")
    void rejectsInvalidExpireHours() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("a".repeat(32), 0));
        assertThrows(IllegalStateException.class, () -> new JwtUtil("a".repeat(32), 24 * 31));
        assertTrue(new JwtUtil("a".repeat(32), 12).getExpireSeconds() == 12 * 3600L);
    }
}
