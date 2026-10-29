package com.learning.lpauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 口令哈希策略。
 *
 * <p>只引入 {@code spring-security-crypto}（纯算法库，无自动配置、无过滤器链），
 * 不使用完整的 Spring Security，避免改动现有自定义拦截器的鉴权模型。</p>
 *
 * <p>BCrypt 自带随机盐与可调 cost，每次编码结果都不同；内置的
 * {@code upgradeEncoding} 还能在调高 cost 后逐步迁移旧哈希。</p>
 */
@Configuration
public class PasswordConfig {

    /**
     * 强度 10 是 Spring Security 的默认值：单次校验约 50~100ms，
     * 在"能抵抗离线爆破"和"登录延迟"之间比较平衡。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
