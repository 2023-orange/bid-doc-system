package com.example.biddoc.folder.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FolderManageScopeEnum {

    SELF("SELF", "仅当前节点"),
    SELF_AND_DESCENDANTS("SELF_AND_DESCENDANTS", "当前节点及其后代");

    private final String code;
    private final String name;

    public static FolderManageScopeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (FolderManageScopeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
