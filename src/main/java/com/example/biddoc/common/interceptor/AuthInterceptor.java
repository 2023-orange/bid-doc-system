package com.example.biddoc.common.interceptor;

import com.example.biddoc.common.constant.UserContext;
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

        UserContext.UserInfo user = UserContext.get();
        if (user == null || !user.isAdmin()) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        return true;
    }
}

