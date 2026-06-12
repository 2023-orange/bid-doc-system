package com.example.biddoc.project.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectStatusEnum {

    NORMAL("NORMAL", "正常"),
    PAUSED("PAUSED", "暂停"),
    ARCHIVED("ARCHIVED", "归档"),
    CANCELLED("CANCELLED", "取消"),
    DELETED("DELETED", "删除");

    private final String code;
    private final String name;
}
