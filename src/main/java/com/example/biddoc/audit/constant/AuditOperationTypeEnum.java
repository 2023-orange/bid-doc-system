package com.example.biddoc.audit.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuditOperationTypeEnum {

    CREATE("CREATE", "创建"),
    UPDATE("UPDATE", "更新"),
    RENAME("RENAME", "重命名"),
    DELETE("DELETE", "删除"),
    BATCH_DELETE("BATCH_DELETE", "批量删除"),
    MOVE("MOVE", "移动"),
    COPY("COPY", "复制"),
    GRANT_ADD("GRANT_ADD", "新增授权"),
    GRANT_REMOVE("GRANT_REMOVE", "移除授权"),
    MANAGER_ADD("MANAGER_ADD", "新增管理员"),
    MANAGER_REMOVE("MANAGER_REMOVE", "移除管理员"),
    FAVORITE("FAVORITE", "收藏"),
    UNFAVORITE("UNFAVORITE", "取消收藏");

    private final String code;
    private final String name;

    public static AuditOperationTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (AuditOperationTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
