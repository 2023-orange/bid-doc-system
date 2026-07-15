package com.example.biddoc.audit.dto.req;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AuditQueryReqDTO {

    private String moduleCode;
    private String bizType;
    private Long bizId;
    private String operationType;
    private Long operatorUserId;
    private Long operatorDeptId;
    private String relatedBizType;
    private Long relatedBizId;
    private String keyword;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Integer page = 1;
    private Integer size = 20;
}
