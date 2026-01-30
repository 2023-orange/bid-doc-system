package com.example.biddoc.common.exception;

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

    // 1. 参数校验异常 (JSON Body)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .findFirst()
                .orElse("参数校验错误");
        return ApiResponse.fail(ErrorCode.PARAM_INVALID, msg);
    }

    // 2. 参数校验异常 (Form Data / Get Params)
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException ex) {
        return ApiResponse.fail(ErrorCode.PARAM_INVALID, "参数绑定失败");
    }

    // 3. JSON 解析失败 (如 boolean 传了 "abc")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleJsonError(HttpMessageNotReadableException ex) {
        return ApiResponse.fail(ErrorCode.PARAM_INVALID, "请求体格式错误");
    }

    // 4. 业务异常
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        log.warn("[业务异常] code={} msg={}", ex.getErrorCode().getCode(), ex.getMessage());
        return ApiResponse.fail(ex.getErrorCode(), ex.getMessage());
    }

    // 5. 404 资源未找到 (需要在 yml 开启 throw-exception-if-no-handler-found)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResponse<Void> handle404(NoHandlerFoundException ex) {
        return ApiResponse.fail(ErrorCode.RESOURCE_NOT_FOUND, "请求路径不存在: " + ex.getRequestURL());
    }

    // 6. 405 方法不支持
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<Void> handle405(HttpRequestMethodNotSupportedException ex) {
        return ApiResponse.fail(ErrorCode.PARAM_INVALID, "请求方法不支持: " + ex.getMethod());
    }

    // 7. 兜底异常
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("[系统异常]", ex);
        return ApiResponse.fail(ErrorCode.SYSTEM_ERROR);
    }
}
