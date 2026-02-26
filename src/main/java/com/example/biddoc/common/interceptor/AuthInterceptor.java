package com.example.biddoc.common.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // 【关键修复】如果是 OPTIONS 请求，直接放行，不校验 Token
        // 这是跨域预检请求，浏览器自动发送，不会带 Token
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 【优化】不依赖 UserContext，直接通过 Sa-Token 校验当前用户是否具有 ADMIN 角色
        // Sa-Token 的 StpInterface 接口来提供角色数据
        if (!StpUtil.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        return true;
    }
}

