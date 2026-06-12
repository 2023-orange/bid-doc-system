package com.example.biddoc.project.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectMemberRoleEnum {

    OWNER("OWNER", "项目负责人"),
    MATERIAL_OWNER("MATERIAL_OWNER", "资料负责人"),
    MEMBER("MEMBER", "项目成员");

    private final String code;
    private final String name;
}
