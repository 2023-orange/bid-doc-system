package com.example.biddoc.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用
    PARAM_INVALID(4001001, "参数校验失败"),
    RESOURCE_CONFLICT(4001002, "资源冲突"),
    BUSINESS_ILLEGAL(4001003, "业务规则非法"),

    // 认证
    AUTH_FAILED(4011001, "认证失败"),
    ACCOUNT_DISABLED(4011002, "账号不可用"),
    LOGIN_LIMITED(4011003, "登录受限"),

    // 权限
    PERMISSION_DENIED(4031001, "权限不足"),

    // 资源
    RESOURCE_NOT_FOUND(4041001, "资源不存在"),

    // 系统
    SYSTEM_ERROR(5001001, "系统异常");

    private final int code;
    private final String message;
}
