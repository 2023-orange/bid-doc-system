package com.example.biddoc.common.config;

import com.example.biddoc.common.interceptor.AuthInterceptor;
import com.example.biddoc.common.interceptor.LoggingInterceptor;
import com.example.biddoc.common.interceptor.TokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoggingInterceptor loggingInterceptor;
    @Autowired
    private TokenInterceptor tokenInterceptor;
    @Autowired
    private AuthInterceptor authInterceptor;

    /**
     * 配置全局 CORS (解决跨域问题)
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 日志拦截器 (所有路径)
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**");

        // 2. Token 拦截器 (排除登录注册、Knife4j 文档资源)
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/favicon.ico"
                );

        // 3. 权限拦截器
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/users/**", "/api/v1/departments/**");
    }
}