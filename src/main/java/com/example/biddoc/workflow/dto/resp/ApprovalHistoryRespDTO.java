package com.example.biddoc.workflow.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ApprovalHistoryRespDTO {

    private Long instanceId;
    private Long documentId;
    private Long submitterUserId;
    private String instanceStatus;
    private String submitComment;
    private OffsetDateTime submittedAt;
    private OffsetDateTime completedAt;
    private Long taskId;
    private Long approverUserId;
    private String taskStatus;
    private String taskComment;
    private OffsetDateTime handledAt;
}
