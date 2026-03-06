package com.example.biddoc.common.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.example.biddoc.auth.constant.RoleCodeEnum;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理接口权限拦截器
 * <p>
 * /api/v1/users/** → 仅 SUPER_ADMIN<br/>
 * /api/v1/departments/** → SUPER_ADMIN 或 DEPT_MANAGER
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        if (path.startsWith("/api/v1/users")) {
            if (!StpUtil.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode())) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED);
            }
        } else if (path.startsWith("/api/v1/departments")) {
            if (!StpUtil.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode())
                    && !StpUtil.hasRole(RoleCodeEnum.DEPT_MANAGER.getCode())) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED);
            }
        }

        return true;
    }
}

