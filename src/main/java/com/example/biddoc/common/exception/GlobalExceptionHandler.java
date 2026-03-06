package com.example.biddoc.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.example.biddoc.common.result.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.net.BindException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .findFirst()
                .orElse("参数校验错误");
        return ApiResponse.fail(ErrorCode.PARAM_INVALID, msg);
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException ex) {
        return ApiResponse.fail(ErrorCode.PARAM_INVALID, "参数绑定失败");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleJsonError(HttpMessageNotReadableException ex) {
        return ApiResponse.fail(ErrorCode.PARAM_INVALID, "请求体格式错误");
    }

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        log.warn("[业务异常] code={} msg={}", ex.getErrorCode().getCode(), ex.getMessage());
        return ApiResponse.fail(ex.getErrorCode(), ex.getMessage());
    }

    // ========== Sa-Token 鉴权异常 ==========

    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<Void> handleNotLogin(NotLoginException ex) {
        log.warn("[未登录] {}", ex.getMessage());
        return ApiResponse.fail(ErrorCode.AUTH_FAILED, "请先登录");
    }

    @ExceptionHandler(NotRoleException.class)
    public ApiResponse<Void> handleNotRole(NotRoleException ex) {
        log.warn("[角色不足] 缺少角色: {}", ex.getRole());
        return ApiResponse.fail(ErrorCode.PERMISSION_DENIED, "角色权限不足");
    }

    @ExceptionHandler(NotPermissionException.class)
    public ApiResponse<Void> handleNotPermission(NotPermissionException ex) {
        log.warn("[权限不足] 缺少权限: {}", ex.getPermission());
        return ApiResponse.fail(ErrorCode.PERMISSION_DENIED, "操作权限不足");
    }

    // ========== 通用异常 ==========

    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResponse<Void> handle404(NoHandlerFoundException ex) {
        return ApiResponse.fail(ErrorCode.RESOURCE_NOT_FOUND, "请求路径不存在: " + ex.getRequestURL());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<Void> handle405(HttpRequestMethodNotSupportedException ex) {
        return ApiResponse.fail(ErrorCode.PARAM_INVALID, "请求方法不支持: " + ex.getMethod());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("[系统异常]", ex);
        return ApiResponse.fail(ErrorCode.SYSTEM_ERROR);
    }
}
