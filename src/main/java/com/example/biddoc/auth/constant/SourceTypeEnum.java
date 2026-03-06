package com.example.biddoc.auth.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色分配来源类型枚举
 */
@Getter
@AllArgsConstructor
public enum SourceTypeEnum {

    MANUAL(1, "手动分配"),
    SYSTEM_INIT(2, "系统初始化"),
    SYNC_IMPORT(3, "同步导入");

    private final int code;
    private final String name;

    public static SourceTypeEnum getByCode(int code) {
        for (SourceTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}

