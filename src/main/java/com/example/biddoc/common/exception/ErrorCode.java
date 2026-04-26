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
    ROLE_NOT_MATCH(4031002, "角色权限不匹配"),
    FOLDER_PERMISSION_DENIED(4032001, "无文件夹操作权限"),

    // 资源
    RESOURCE_NOT_FOUND(4041001, "资源不存在"),
    FOLDER_NOT_FOUND(4042001, "文件夹不存在"),
    FOLDER_PARENT_NOT_FOUND(4042002, "父文件夹不存在"),

    // Folder
    FOLDER_NAME_DUPLICATED(4002001, "同级文件夹名称已存在"),
    FOLDER_LEVEL_EXCEEDED(4002002, "文件夹层级超出限制"),
    FOLDER_HAS_CHILDREN(4002003, "当前文件夹存在子节点"),

    // Audit
    AUDIT_RECORD_FAILED(5003001, "审计记录失败"),
    AUDIT_QUERY_FAILED(5003002, "审计查询失败"),

    // 系统
    SYSTEM_ERROR(5001001, "系统异常");

    private final int code;
    private final String message;
}
