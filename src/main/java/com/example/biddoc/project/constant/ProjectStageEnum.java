package com.example.biddoc.project.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectStageEnum {

    COLLECTING("COLLECTING", "资料收集中"),
    REVIEWING("REVIEWING", "资料审核中"),
    COMPILING("COMPILING", "投标文件编制中"),
    WAITING_SUBMIT("WAITING_SUBMIT", "待提交"),
    SUBMITTED("SUBMITTED", "已提交"),
    ARCHIVED("ARCHIVED", "已归档"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String name;
}
