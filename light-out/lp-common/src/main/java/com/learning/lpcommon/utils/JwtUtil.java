package com.learning.lpcommon.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

/**
 * JWT 签发与校验。
 *
 * <p>密钥只从配置读取（环境变量 {@code JWT_SECRET}），不再硬编码在源码里。
 * 硬编码密钥等于把"签发任意身份令牌"的能力随源码一起分发出去——拿到源码就能
 * 自己签一个 {@code roleCode=super_admin} 的 token。</p>
 *
 * <p>密钥缺失或短于 32 字节时，构造阶段直接抛异常让应用启动失败（fail-fast），
 * 避免带着弱密钥被"默认值"悄悄带上线。</p>
 */
@Component
public class JwtUtil {

    /**
     * 业务载荷统一放在这个 claim 下。
     * 保持与原实现一致，前端与 {@code UserContext} 的取值约定不变。
     */
    private static final String CLAIMS_CLAIM = "claims";

    private static final String ISSUER = "learning-platform";

    /** HS256 密钥长度下限：32 字节 = 256 bit，低于此值可被离线暴力破解 */
    private static final int MIN_SECRET_BYTES = 32;

    private static final String BEARER_PREFIX = "Bearer ";

    /** 令牌有效期上限，防止配置失误签出长命令牌（30 天） */
    private static final long MAX_EXPIRE_HOURS = 24 * 30;

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expireSeconds;

    public JwtUtil(@Value("${jwt.secret:}") String secret,
                   @Value("${jwt.expire-hours:12}") long expireHours) {
        this.algorithm = Algorithm.HMAC256(requireStrongSecret(secret));
        // 校验时强制校验 issuer，拒绝其他系统签出的同算法令牌
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
        if (expireHours <= 0 || expireHours > MAX_EXPIRE_HOURS) {
            throw new IllegalStateException(
                    "jwt.expire-hours 必须在 1~" + MAX_EXPIRE_HOURS + " 之间，当前配置为 " + expireHours);
        }
        this.expireSeconds = expireHours * 3600L;
    }

    /**
     * 签发令牌。
     *
     * @param claims 业务载荷（userId、roleCode 等），调用方不要放敏感信息
     */
    public String createToken(Map<String, Object> claims) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(expireSeconds, ChronoUnit.SECONDS)))
                .withClaim(CLAIMS_CLAIM, claims)
                .sign(algorithm);
    }

    /**
     * 校验并解析令牌。
     *
     * <p>签名错误、过期、issuer 不符、缺少业务载荷都会抛异常，由调用方转成 401。</p>
     */
    public Map<String, Object> parseToken(String rawToken) {
        String token = stripBearer(rawToken);
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("缺少访问令牌");
        }
        DecodedJWT jwt = verifier.verify(token);
        Map<String, Object> claims = jwt.getClaim(CLAIMS_CLAIM).asMap();
        if (claims == null || claims.isEmpty()) {
            throw new IllegalArgumentException("令牌缺少业务载荷");
        }
        return claims;
    }

    /** 当前配置的令牌有效期（秒），便于需要时下发或记录 */
    public long getExpireSeconds() {
        return expireSeconds;
    }

    private static String requireStrongSecret(String secret) {
        String value = secret == null ? "" : secret.trim();
        int length = value.getBytes(StandardCharsets.UTF_8).length;
        if (length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("""
                    JWT 密钥未配置或过短：jwt.secret 至少需要 32 字节（当前 %d 字节）。
                    请设置环境变量 JWT_SECRET 后重启，例如：
                      Windows PowerShell : $env:JWT_SECRET = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
                      Linux / macOS      : export JWT_SECRET="$(openssl rand -base64 48)"
                      IDEA 运行配置       : Run/Debug Configurations -> Environment variables 中添加 JWT_SECRET
                    """.formatted(length));
        }
        return value;
    }

    /** 兼容 `Bearer xxx` 与裸 token 两种传法 */
    private static String stripBearer(String token) {
        if (token == null) {
            return null;
        }
        String trimmed = token.trim();
        if (trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return trimmed.substring(BEARER_PREFIX.length()).trim();
        }
        return trimmed;
    }
}
