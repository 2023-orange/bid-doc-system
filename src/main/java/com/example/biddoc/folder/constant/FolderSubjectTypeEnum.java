package com.example.biddoc.folder.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FolderSubjectTypeEnum {

    USER("USER", "用户"),
    ROLE("ROLE", "角色"),
    DEPT("DEPT", "部门");

    private final String code;
    private final String name;

    public static FolderSubjectTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (FolderSubjectTypeEnum value : values()) {
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
