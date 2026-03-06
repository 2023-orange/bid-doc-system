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

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        try {
            StpUtil.checkLogin();

            SysUser saUser = (SysUser) StpUtil.getSession().get("user");

            if (saUser != null) {
                @SuppressWarnings("unchecked")
                List<String> roleCodes = (List<String>) StpUtil.getSession().get("roleCodes");
                if (roleCodes == null) {
                    roleCodes = Collections.emptyList();
                }

                UserContext.UserInfo user = new UserContext.UserInfo(
                        saUser.getId(),
                        saUser.getUsername(),
                        roleCodes,
                        saUser.getDeptId()
                );

                UserContext.set(user);
                MDC.put("user", user.getUsername() + "(" + user.getUserId() + ")");
            }

            return true;
        } catch (NotLoginException e) {
            log.warn("Sa-Token解析/校验失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}