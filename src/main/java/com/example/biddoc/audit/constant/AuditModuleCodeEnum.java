package com.example.biddoc.audit.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuditModuleCodeEnum {

    FOLDER("FOLDER", "文件夹模块"),
    DOCUMENT("DOCUMENT", "文档模块"),
    PROJECT("PROJECT", "项目模块"),
    WORKFLOW("WORKFLOW", "审批流程模块");

    private final String code;
    private final String name;

    public static AuditModuleCodeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (AuditModuleCodeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
