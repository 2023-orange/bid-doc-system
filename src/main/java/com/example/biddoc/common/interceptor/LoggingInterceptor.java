package com.example.biddoc.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC; // 引入 MDC
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID = "traceId";
    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 生成唯一的 TraceId，方便链路追踪
        String traceId = UUID.randomUUID().toString().replace("-", "");
        MDC.put(TRACE_ID, traceId);

        // 2. 记录请求开始时间
        request.setAttribute(START_TIME, System.currentTimeMillis());

        // 3. 打印请求日志 (精简版)
        log.info("==> REQ: {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 1. 计算耗时
        long startTime = (Long) request.getAttribute(START_TIME);
        long duration = System.currentTimeMillis() - startTime;

        // 2. 获取当前用户（从 MDC 中获取，由 TokenInterceptor 放入）
        String user = MDC.get("user");

        // 3. 打印响应日志 (包含耗时和状态码)
        log.info("<== RES: {} {} [{}] [{}ms] User:{}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration,
                user != null ? user : "Anonymous");

        // 4. 【非常重要】清除 MDC，防止线程复用导致 TraceId 混乱
        MDC.clear();
    }
}