package com.example.biddoc.audit.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class AuditLogRespDTO {

    private Long id;
    private String moduleCode;
    private String bizType;
    private Long bizId;
    private String operationType;
    private Long operatorUserId;
    private String operatorName;
    private Long operatorDeptId;
    private String requestId;
    private String objectName;
    private String actionSummary;
    private String relatedBizType;
    private Long relatedBizId;
    private String clientIp;
    private String userAgent;
    private OffsetDateTime operationTime;
    private Map<String, Object> beforeData;
    private Map<String, Object> afterData;
    private Map<String, Object> extraData;
    private OffsetDateTime createdAt;
    private String createdBy;
}
