package com.example.biddoc.project.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChecklistItemStatusEnum {

    PENDING_COLLECT("PENDING_COLLECT", "待收集"),
    PENDING_REVIEW("PENDING_REVIEW", "待审核"),
    NEED_SUPPLEMENT("NEED_SUPPLEMENT", "需补充"),
    COMPLETE("COMPLETE", "已收齐"),
    ARCHIVED("ARCHIVED", "已归档");

    private final String code;
    private final String name;
}
