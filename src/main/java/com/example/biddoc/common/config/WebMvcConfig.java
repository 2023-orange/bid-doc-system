package com.example.biddoc.common.config;

import com.example.biddoc.common.interceptor.AuthInterceptor;
import com.example.biddoc.common.interceptor.LoggingInterceptor;
import com.example.biddoc.common.interceptor.TokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoggingInterceptor loggingInterceptor;
    @Autowired
    private TokenInterceptor tokenInterceptor;
    @Autowired
    private AuthInterceptor authInterceptor;

    // 统一管理白名单
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/doc.html", "/webjars/**", "/v3/api-docs/**", "/favicon.ico", // Swagger/Knife4j
            "/error" // Spring Boot 默认错误页
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 日志拦截器 (最外层，先记录进入，最后记录离开)
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")
                .order(1); // 显式指定顺序

        // 2. Token 拦截器 (解析身份)
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(EXCLUDE_PATHS)
                .order(2);

        // 3. 权限拦截器 (校验具体权限)
        if (authInterceptor != null) {
            registry.addInterceptor(authInterceptor)
                    .addPathPatterns("/api/v1/users/**", "/api/v1/departments/**") // 敏感接口
                    .order(3);
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 项目中的所有接口都支持跨域
        registry.addMapping("/**")
                // 1. 允许所有来源模式：
                // 使用 allowedOriginPatterns("*") 是 SpringBoot 2.4+ 的推荐写法
                // 它比 allowedOrigins("*") 更灵活，且允许 allowCredentials(true)
                // 这意味着你同事不管是在 localhost:3000 还是 ip:端口 访问，都能通过
                .allowedOriginPatterns("*")

                // 2. 允许的请求方法：
                // 显式列出通常更安全，但 "*" 也可以。确保包含 OPTIONS，这是预检请求必须的
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")

                // 3. 允许的头部信息：
                // 允许前端发送 Authorization, Token 等自定义 Header
                .allowedHeaders("*")

                // 4. 暴露的头部信息 (重要)：
                // 如果前端需要从 Response Header 中获取 token 或其他信息，必须在这里暴露
                .exposedHeaders("*")

                // 5. 允许携带凭证 (Cookies/Auth头)：
                // 前端联调通常需要携带 Token，此项必须为 true
                .allowCredentials(true)

                // 6. 预检请求缓存时间 (秒)：
                // 1小时内不需要再次发送 OPTIONS 请求，减少 cpolar 隧道带宽消耗
                .maxAge(3600);
    }
}