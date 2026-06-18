package com.example.biddoc.workflow.dto.req;

import lombok.Data;

@Data
public class ApprovalConditionSaveReqDTO {

    private String conditionCode;
    private String fieldName;
    private String operator;
    private String compareValue;
    private String targetNodeCode;
    private Integer sortOrder;
}
