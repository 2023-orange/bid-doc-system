package com.example.biddoc.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置。
 * Goal-0: 引入 BCrypt 密码加密，替代明文存储。
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 全局密码编码器。
     * 使用 BCrypt，默认 cost=10（BCryptPasswordEncoder 构造器默认值，单次 hash 约 100ms，符合安全/性能权衡）。
     * 与 Sa-Token 鉴权链独立，只用于 sys_user.password 字段的写入与比对。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
