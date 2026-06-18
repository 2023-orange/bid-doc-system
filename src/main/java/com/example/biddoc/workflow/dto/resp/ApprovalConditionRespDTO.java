package com.example.biddoc.workflow.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ApprovalConditionRespDTO {

    private Long id;
    private Long definitionId;
    private Long nodeId;
    private String conditionCode;
    private String fieldName;
    private String operator;
    private String compareValue;
    private String targetNodeCode;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
