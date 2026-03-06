package com.example.biddoc.auth.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统角色编码枚举，所有角色判断必须引用此枚举，禁止硬编码魔法值
 */
@Getter
@AllArgsConstructor
public enum RoleCodeEnum {

    SUPER_ADMIN("SUPER_ADMIN", "超级管理员"),
    FOLDER_ADMIN("FOLDER_ADMIN", "文件夹管理员"),
    DEPT_MANAGER("DEPT_MANAGER", "部门经理"),
    EMPLOYEE("EMPLOYEE", "普通员工");

    private final String code;
    private final String name;

    public static RoleCodeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (RoleCodeEnum role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }

    public static boolean isValid(String code) {
        return getByCode(code) != null;
    }
}

