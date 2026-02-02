package com.example.biddoc.common.interceptor;

import com.example.biddoc.common.constant.HeaderConstants;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode; // 假设你有 AUTH_EXPIRED 或 AUTH_INVALID
import com.example.biddoc.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 放行 OPTIONS 请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 获取并校验 Header
        String authHeader = request.getHeader(HeaderConstants.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader)) {
            throw new BusinessException(ErrorCode.AUTH_FAILED); // 未登录
        }

        // 3. 提取 Token
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        try {
            // 4. 解析 Token (这里加 try-catch 是为了处理 Token 过期或篡改)
            UserContext.UserInfo user = JwtUtil.parse(token);

            // 5. 设置业务上下文
            UserContext.set(user);

            // 6. 【优化】设置日志上下文 (让 LoggingInterceptor 能打印出是谁)
            if (user != null) {
                MDC.put("user", user.getUsername() + "(" + user.getUserId() + ")");
            }

            return true;
        } catch (Exception e) {
            log.warn("Token解析失败: {}", e.getMessage());
            // 抛出具体的业务异常，由全局异常处理器捕获返回 401
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
        // 注意：MDC 不在这里清，而在 LoggingInterceptor 清，因为 Logging 在最外层
    }
}