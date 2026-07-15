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
 * /api/v1/departments/** → 查询允许 SUPER_ADMIN 或 DEPT_MANAGER，写操作按管理风险收紧
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
            if (!hasDepartmentPermission(request)) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED);
            }
        }

        return true;
    }

    private boolean hasDepartmentPermission(HttpServletRequest request) {
        if (StpUtil.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode())) {
            return true;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();
        boolean deptManager = StpUtil.hasRole(RoleCodeEnum.DEPT_MANAGER.getCode());

        // TODO 部门经理的数据范围暂未实现；当前仅预留查询和启停入口，创建、编辑、删除仍需超级管理员。
        if ("GET".equalsIgnoreCase(method)) {
            return deptManager;
        }
        return deptManager
                && "PATCH".equalsIgnoreCase(method)
                && path.matches("/api/v1/departments/\\d+/status");
    }
}

