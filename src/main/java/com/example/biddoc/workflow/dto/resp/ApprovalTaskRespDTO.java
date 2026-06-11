package com.example.biddoc.workflow.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ApprovalTaskRespDTO {

    private Long taskId;
    private Long instanceId;
    private Long documentId;
    private Long approverUserId;
    private String status;
    private String comment;
    private OffsetDateTime createdAt;
    private OffsetDateTime handledAt;
}
