package com.example.biddoc.workflow.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ApprovalTaskRespDTO {

    private Long taskId;
    private Long instanceId;
    private Long documentId;
    private String definitionName;
    private String nodeName;
    private String actionType;
    private String actionComment;
    private OffsetDateTime actionTime;
    private Long handlerUserId;
    private String businessTitle;
    private String bizModule;
    private Long bizId;
    private String documentName;
    private String documentNo;
    private String projectName;
    private String checklistItemName;
    private String submitterName;
    private Long approverUserId;
    private String approverName;
    private String handlerName;
    private String instanceStatus;
    private String instanceStatusName;
    private String status;
    private String comment;
    private Boolean canApprove;
    private Boolean canReject;
    private Boolean canTransfer;
    private Boolean canAddSign;
    private Boolean canWithdraw;
    private Boolean canTerminate;
    private OffsetDateTime createdAt;
    private OffsetDateTime handledAt;
}
