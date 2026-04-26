package com.example.biddoc.folder.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FolderPermissionCodeEnum {

    FOLDER_VIEW("FOLDER_VIEW", "查看文件夹"),
    FOLDER_CREATE("FOLDER_CREATE", "创建子文件夹"),
    FOLDER_EDIT("FOLDER_EDIT", "编辑文件夹"),
    FOLDER_RENAME("FOLDER_RENAME", "重命名文件夹"),
    FOLDER_DELETE("FOLDER_DELETE", "删除文件夹"),
    FOLDER_MOVE("FOLDER_MOVE", "移动文件夹"),
    FOLDER_COPY("FOLDER_COPY", "复制文件夹"),
    FOLDER_GRANT("FOLDER_GRANT", "授权管理"),
    FOLDER_MANAGER_SET("FOLDER_MANAGER_SET", "管理员设置"),
    FOLDER_FAVORITE("FOLDER_FAVORITE", "收藏文件夹"),
    FOLDER_AUDIT_VIEW("FOLDER_AUDIT_VIEW", "查看审计日志");

    private final String code;
    private final String name;

    public static FolderPermissionCodeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (FolderPermissionCodeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static boolean isValid(String code) {
        return getByCode(code) != null;
    }
}
