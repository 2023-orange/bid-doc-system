package com.example.biddoc.common.interceptor;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
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

        try {
            // 2. 【优化】直接交由 Sa-Token 校验登录状态 (自动读取Header，自动校验过期和无感续期)
            StpUtil.checkLogin();

            // 3. 【优化】从 Sa-Token 的 Redis Session 中获取高速缓存的用户信息
            SysUser saUser = (SysUser) StpUtil.getSession().get("user");

            if (saUser != null) {
                // 4. 组装旧版的 UserInfo，兼容老代码
                String roleStr = (saUser.getRoleId() != null && saUser.getRoleId() == 1L) ? "ADMIN" : "USER";
                UserContext.UserInfo user = new UserContext.UserInfo(saUser.getId(), saUser.getUsername(), roleStr);

                // 5. 设置业务上下文和日志 MDC
                UserContext.set(user);
                MDC.put("user", user.getUsername() + "(" + user.getUserId() + ")");
            }

            return true;
        } catch (NotLoginException e) {
            log.warn("Sa-Token解析/校验失败: {}", e.getMessage());
            // 抛出你原有的业务异常，由 GlobalExceptionHandler 捕获
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}