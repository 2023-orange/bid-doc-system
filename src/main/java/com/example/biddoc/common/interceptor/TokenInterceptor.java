package com.example.biddoc.common.interceptor;

import com.example.biddoc.common.constant.HeaderConstants;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 如果是 OPTIONS 请求直接放行（处理 CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader(HeaderConstants.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader)) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }

        // 处理 Bearer 前缀
        String token = authHeader;
        if (authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        UserContext.UserInfo user = JwtUtil.parse(token);
        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
